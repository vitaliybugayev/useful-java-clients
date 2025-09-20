package client.temporal;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;

import java.util.List;

@WorkflowInterface
public interface TemporalWorkflowExample {

    @QueryMethod(name = "GetState")
    String getState();

    @QueryMethod(name = "GetEventHistory")
    List<String> getEventHistory();
}
