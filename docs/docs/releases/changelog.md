# Changelog

Embed the changelog from the GitHub repository here.

### v0.3.0
- **Workflow lifecycle**: Lifecycle is divided into 3 stages: _definition_, _build_, _run_. This feature allows you to define parameters to customize the workflow behavior for each stage, giving you the flexibility to redefine, rebuild, and rerun workflows at any time.
- **Workflow patterns**: This version supports multiple workflow patterns, including `one-way`, `parallelization`, `routing`, `branching` and `conditional` inspired on [Modular RAG AI paper](https://arxiv.org/pdf/2407.21059v1). All of these patterns can be used at _definition_, _build_ and _run_ time, except for _parallelization_ which is not supported at _run_ time.
- **Node types**: Adds new node types: `Split`, `Merge`, `Parallel`, `Conditional` with a list of expected nodes to be returned. These nodes allow you to define complex workflows patterns with multiple transitions between nodes.
- **Debug workflow state**: Once workflow runs you can get a list of `ComputedTransition` with all details of the workflow execution. This feature allows you to debug the flow of your application and trace the inputs, outputs, execution order, datetime of each node.
- **Workflow JIT modification**: You can put edges `myworkflow.putEdge(..)`, add more nodes `myworkflow.addNode(..)` and override the start node `myworkflow.startNode(..)` at Just-in-time after workflow runs. This feature allows you to modify the workflow behavior dynamically during execution.
- **Workflow visualization**: You can generate the workflow image at definition time and at runtime. This feature allows you to visualize the transitions computed of your app workflow. Both kinds of images can be generated in a given path `File` or as `BufferedImage` to be displayed in a java _notebook_. Also, you can use `StyleGraph.SKETCHY` as `StyleAttribute` to generate workflow images with [excalidraw](https://github.com/excalidraw/excalidraw) style. This style is supported in `Graphviz` implementation only.

### v0.2.0
- **Graph-core**: The engine supports create `Nodes`, `Conditional Nodes`, `Edges`, and workflows as a graph. This feature allows you to define custom workflows with multiple `Transitions` between nodes such as one-way, round trip and recursive.
- **Run workflow**: jAI Workflow supports synchronized `workflow.run()` and streaming `worflow.runStream()` runs the outputs as they are produced by each node. This last feature allows for real-time processing and response in your application.
- **Integration**: [LangChain4j](https://docs.langchain4j.dev/) integration, enabling you to define custom workflows using all the features that LangChain4j offers. This integration provides a comprehensive toolset for building advanced AI applications to integrate with multiple LLM providers and models.
- **Visualization**: The engine supports the generation of workflow images. This feature allows you to visualize the flow computed of your app workflow. By Default it uses `Graphhviz` lib to generate the image, but you implement your own image generator on `GraphImageGenerator.java` interface.

