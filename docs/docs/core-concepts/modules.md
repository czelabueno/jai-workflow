# Modules

Module is a collection of nodes grouped together to perform a higher-level function. Modules can be reused across different [workflows](workflows.md) as a [remote module](remote-module.md), providing modularity and reusability.

## Advantages of using modules

- A workflow can be composed of multiple modules, each responsible for a specific task or set of tasks. This modular approach allows for better organization and maintainability of the workflow.
- A Workflow can be belonged to a module, and a module can be a workflow. This means that a module can contain other modules, allowing for nested workflows. 

## When to use modules?

- When you have a set of [nodes](nodes.md) that are frequently used together, it is a good idea to group them into a module. This allows you to reuse the module in different workflows without having to duplicate the nodes.
- When you have a complex workflow that can be broken down into smaller, more manageable pieces, it is a good idea to create modules for each piece. This allows you to focus on one piece at a time and makes it easier to understand the overall workflow.
- When you want to create a library of reusable components that can be shared across different projects, it is a good idea to create modules for each component. This allows you to easily share and reuse the components without having to duplicate the code.
- When you want to create a module that can be used in different workflows, it is a good idea to create a module for each workflow. This allows you to easily share and reuse the workflows without having to duplicate the code.

!!! note "Coming soon"
    This section is a work in progress. We are actively working on improving the documentation and will provide more details soon. In the meantime, feel free to explore the other sections of the documentation for more information on using jAI Workflow.
