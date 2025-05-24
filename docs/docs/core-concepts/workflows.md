# Workflows

Workflows are a way to **orchestrate** the execution flow of **agents, nodes and modules** in a directed graph. They allow you a **full control** over the execution flow, enabling you to define complex workflows with multiple directions such as one-way, round trip, loop, recursive and more. This feature allows for intricate control over the flow of your process and/or application.

## Workflow patterns
A workflow pattern is a specific arrangement of nodes and transitions that defines the flow of control within a workflow. It can be used to represent different types of workflows, such as sequential, parallel, conditional, or iterative workflows. The pattern can be customized to suit the specific needs of the application or process being modeled.

1. **Sequential Workflow**: A linear flow of control where each node is executed one after the other. This is the simplest workflow pattern and is suitable for straightforward processes.
2. **Parallel Workflow**: A flow of control where multiple nodes are executed simultaneously. This pattern is useful for processes that can be performed concurrently, improving efficiency and reducing overall execution time.
3. **Conditional Workflow**: A flow of control that branches based on certain conditions. This pattern allows for dynamic routing of the workflow based on the state of the application or process.
4. **Iterative Workflow**: A flow of control that repeats certain nodes or transitions based on specific conditions. This pattern is useful for processes that require multiple iterations or loops.
5. **Recursive Workflow**: A flow of control that calls itself, allowing for complex workflows that can adapt to changing conditions. This pattern is useful for processes that require self-referential behavior or dynamic adjustments.

!!! note "Custom workflow pattern"
    jAI Workflow allow you to create **custom workflow patterns** by combining these basic patterns or create a **new one from scratch** using core components. You can create workflows that are tailored to the specific needs of your application or process, providing flexibility and adaptability.
