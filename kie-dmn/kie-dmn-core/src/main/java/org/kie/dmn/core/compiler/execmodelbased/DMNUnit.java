package org.kie.dmn.core.compiler.execmodelbased;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.runtime.rule.RuleUnit;
import org.kie.api.runtime.rule.RuleUnitExecutor;
import org.kie.dmn.api.core.DMNDecisionResult;
import org.kie.dmn.api.core.DMNDecisionResult.DecisionEvaluationStatus;
import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.core.impl.DMNDecisionResultImpl;
import org.kie.dmn.feel.lang.EvaluationContext;
import org.kie.dmn.feel.runtime.decisiontables.DecisionTable;
import org.kie.dmn.feel.runtime.decisiontables.HitPolicy;
import org.kie.dmn.feel.runtime.decisiontables.Indexed;
import org.kie.dmn.feel.runtime.events.HitPolicyViolationEvent;

import static java.util.stream.Collectors.toList;

public abstract class DMNUnit implements RuleUnit {

    private EvaluationContext evalCtx;

    private HitPolicy hitPolicy;
    private DecisionTable decisionTable;

    protected Object result;

    private DecisionTableEvaluator evaluator;

    private List<FEELEvent> events;

    public Object getResult() {
        return result;
    }

    DMNDecisionResult execute( String decisionId, RuleUnitExecutor executor) {
        executor.run(this);
        return new DMNDecisionResultImpl(decisionId, decisionTable.getName(), DecisionEvaluationStatus.SUCCEEDED, getResult(), Collections.emptyList());
    }

    protected Object getValue( int pos ) {
        return evaluator.getInputs()[pos];
    }

    DMNUnit setEvalCtx( EvaluationContext evalCtx ) {
        this.evalCtx = evalCtx;
        return this;
    }

    DMNUnit setHitPolicy( HitPolicy hitPolicy ) {
        this.hitPolicy = hitPolicy;
        return this;
    }

    DMNUnit setDecisionTable( DecisionTable decisionTable ) {
        this.decisionTable = decisionTable;
        return this;
    }

    DMNUnit setDecisionTableEvaluator( DecisionTableEvaluator evaluator ) {
        this.evaluator = evaluator;
        return this;
    }

    public DecisionTableEvaluator getEvaluator() {
        return evaluator;
    }

    protected Object applyHitPolicy( List<Object>... results) {
        if (evaluator.getIndexes().isEmpty()) {
            if( hitPolicy.getDefaultValue() != null ) {
                return hitPolicy.getDefaultValue();
            }
            events = new ArrayList<>();
            events.add( new HitPolicyViolationEvent(
                    FEELEvent.Severity.WARN,
                    "No rule matched for decision table '" + decisionTable.getName() + "' and no default values were defined. Setting result to null.",
                    decisionTable.getName(),
                    Collections.EMPTY_LIST ) );
        }

        List<? extends Indexed> matches = evaluator.getIndexes().stream().map( i -> (Indexed ) () -> i ).collect( toList() );
        if (results.length == 1) {
            return hitPolicy.getDti().dti( evalCtx, decisionTable, matches, results[0] );
        }

        int resultSize = results[0].size();
        List<Object> resultsAsMap = new ArrayList<>();
        for (int i = 0; i < resultSize; i++) {
            Map<String, Object> map = new HashMap<>();
            for (int j = 0; j < results.length; j++) {
                map.put( decisionTable.getOutputs().get(j).getName(), results[j].get(i) );
            }
            resultsAsMap.add(map);
        }
        return hitPolicy.getDti().dti( evalCtx, decisionTable, matches, resultsAsMap );
    }

    public List<FEELEvent> getEvents() {
        return events == null ? Collections.emptyList() : events;
    }
}
