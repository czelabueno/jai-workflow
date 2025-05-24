# Transition

A `Transition` is a directed edge between two nodes or two modules, representing the flow of control from one node to another. Transitions can be **sequential, parallel or loop**, allowing for dynamic routing based on the state of the workflow.

## Transition types
- **Sequential Transition**: A transition that connects two nodes or modules in a linear fashion, allowing for the execution of one node after another. This is the most common type of transition and is used in most workflows.
- **Parallel Transition**: A transition that connects two nodes or modules in a parallel fashion, allowing for the execution of multiple nodes simultaneously. This type of transition is useful for workflows that require concurrent processing.
- **Loop Transition**: A transition that connects a node or module back to itself, allowing for the execution of the same node multiple times. This type of transition is useful for workflows that require iterative processing or repeated actions.

## Transition conditions
- **Condition-based Transition**: A transition that is triggered based on result of a `ConditionalNode`. This type of transition allows for dynamic routing of the workflow based on the state of the application or process.
- **Loop-based Transition**: This type of transition allows for iterative processing or repeated actions within the workflow.


!!! note "Coming soon"
    This section is a work in progress. We are actively working on improving the documentation and will provide more details soon. In the meantime, feel free to explore the other sections of the documentation for more information on using jAI Workflow.
