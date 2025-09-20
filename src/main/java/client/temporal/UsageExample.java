package client.temporal;

public class UsageExample {

    //Example usage of TemporalClient to get the state of a workflow
    public void example() {
        TemporalClient temporalClient = new TemporalClient("temporal-service-address",
                "temporal-cert-key-path",
                "temporal-cert-path", "temporal-namespace");

        //Assuming we have TemporalWorkflow with WorkflowId "12213f5e-42fe-4e9a-aef6-d676d7705851"
        var workflow = temporalClient.getWorkflow(TemporalWorkflowExample.class,
                "12213f5e-42fe-4e9a-aef6-d676d7705851");

        //Assuming we have TemporalWorkflow with RequestId "bbc18ac7-f173-4b2e-83a2-a6b33dd3cb98"
        var workflowByRequestID = temporalClient
                .getWorkflowByRequestID(TemporalWorkflowExample.class,
                        "bbc18ac7-f173-4b2e-83a2-a6b33dd3cb98");

        // Call query method to check the state of the workflow
        var workFlowState = workflow.getState();
        var eventHistory = workflow.getEventHistory();
        var first = workflowByRequestID.stream().findFirst();
        first.isPresent();
        first.get().getState();


        System.out.println("Current workflow state: " + workFlowState);
        System.out.println("Current eventHistory state: " + eventHistory);
    }
}
