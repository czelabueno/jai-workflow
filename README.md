# JavAI Workflow 🦜🔂☕: Build programmatically custom agentic workflows, AI Agents, RAG systems for java
[![Build Status](https://github.com/czelabueno/langchain4j-workflow/actions/workflows/ci.yaml/badge.svg)](https://github.com/czelabueno/langchain4j-workflow/actions/workflows/ci.yaml)
[![Version](https://img.shields.io/maven-central/v/io.github.czelabueno/jai-workflow-parent?logo=apachemaven)](https://search.maven.org/#search|gav|1|g:"io.github.czelabueno"%20AND%20a:"jai-workflow-core")
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

An open-source Java library to build, package, integrate, orchestrate and monitor agentic AI systems for java developers 💡

![Standalone Architecture](docs/workflow-code-black.png)

<details>
  <summary>☕ Java code</summary>

```java
class MyStatefulBean extends AbstractStatefulBean {
  private List<String> documents;
  // other additional input/output fields that you want to store
}

StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName(GPT_4_O_MINI)
        .build();

var parserDocsNode = Node.from("Docs", obj -> transformDocsFromUrl(obj, uris));
var retrieveNode = Node.from("Vector DB", obj -> extractRelevantDocumentsFromVectorDB(obj));
var generateAnswerNode = StreamingNode.from("Generate",obj -> generateAnswer(obj), model);

var jAiWorkflow = new DefaultJAiWorkflow<MyStatefulBean>(
        new MyStatefulBean(),
        List.of(Transition.from(parserDocsNode, retrieveNode),
                Transition.from(retrieveNode, Conditional.eval("enoughData", 
                        obj -> obj.isEnoughData() ? generateAnswerNode : parserDocsNode, 
                        List.of(parserDocsNode, generateAnswerNode))),
                Transition.from(generateAnswerNode, WorkflowStateName.END)),
        retrieveNode, // Start node
        true // Run in streaming mode
);

// Chat with your JAiWorkflow
var question = "Summarizes the importance of building AI agentic systems";
Flux<String> response = jAiWorkflow.answerStream(question);
```
</details>

> 🌟 **Starring me**: If you find this repository beneficial, don't forget to give it a star! 🌟 It's a simple way to show your appreciation and help this project grow!

## Overview
JavAI Workflow (named initially Langchain4j-workflow) is a dynamic, stateful workflow engine crafted as a Java library. It empowers java developers with granular control over the orchestrated workflows as a graph, iteratively, with cycles, flexibility, control, and conditional decisions. This engine is a game-changer for building sophisticated AI applications, such as multiples RAG-based approaches using modern paradigms and agent architectures. It enables the crafting of custom behavior, leading to a significant reduction in hallucinations and an increase in response reliability.

jAI Workflow is influenced by [LangFlow](https://github.com/langflow-ai/langflow), [LangGraph](https://langchain-ai.github.io/langgraph/tutorials/introduction/), [Graphviz](https://graphviz.gitlab.io/Gallery/directed/).

## Anatomy
![Workflow Image](docs/jai-worflow-anatomy.png)

<details>
  <summary>Node, Module, and Workflow Definitions</summary>

### Node
A `Node` represents a single unit of work within the workflow. It encapsulates a specific function or task that processes the stateful bean and updates it. Nodes can be synchronous or asynchronous (streaming).

### Module
A `Module` is a collection of nodes grouped together to perform a higher-level function. Modules can be reused across different workflows, providing modularity and reusability.

### Workflow
A `Workflow` is a directed graph of nodes, modules and edges that defines the sequence of operations to be performed. It manages the state transitions and execution flow, ensuring that each node processes the stateful bean in the correct order.
</details>

## Principles:
- **Java-based** jAI workflows are configuration as code (java) and agnostic to AI models, enabling you can define custom advanced and dynamic workflows just writing Java code.
- **Stateful**: jAI Workflow is a stateful engine, enabling you to design custom states as POJO and transitions. This feature provides a robust foundation for managing the flow and state of your application.
- **Graph-Based**: The workflow is graph-based, offering the flexibility to define custom workflows with multiple directions such as one-way, round trip, loop, recursive and more. This feature allows for intricate control over the flow of your application.
- **Flexible**: jAI Workflow is designed with flexibility in mind. You can define custom workflows, modules or agents to build RAG systems as LEGO-like. A module can be decoupled and integrated in any other workflow.
- **AI Ecosystem integration**: jAI workflow is designed to be potentially integrable with any emerging AI technology in the future for java.
- **Publish as API**: jAI Workflow can be published as an API as entrypoint once you have defined your custom workflow. This feature allows you to expose your workflow as a service.
- **Observability**: jAI Workflow provides observability features to monitor the execution of the workflow, trace inputs and outputs, and debug the flow of your application.
- **Scalable**: jAI Workflow can be deployed as any java project as standalone or distributed through containers in any cloud provider or kubernetes environment. Each module can run in a different JVM env or container for scalability in production environments.

## 🚀 Features
### v0.3.0 Features
- **Workflow lifecycle**: Lifecycle is divided into 3 stages: _definition_, _build_, _run_. This feature allows you to define parameters to customize the workflow behavior for each stage, giving you the flexibility to redefine, rebuild, and rerun workflows at any time.
- **Workflow patterns**: This version supports multiple workflow patterns, including `one-way`, `parallelization`, `routing`, `branching` and `conditional` inspired on [Modular RAG AI paper](https://arxiv.org/pdf/2407.21059v1). All of these patterns can be used at _definition_, _build_ and _run_ time, except for _parallelization_ which is not supported at _run_ time.
- **Node types**: Adds new node types: `Split`, `Merge`, `Parallel`, `Conditional` with a list of expected nodes to be returned. These nodes allow you to define complex workflows patterns with multiple transitions between nodes.
- **Debug workflow state**: Once workflow runs you can get a list of `ComputedTransition` with all details of the workflow execution. This feature allows you to debug the flow of your application and trace the inputs, outputs, execution order, datetime of each node.
- **Workflow JIT modification**: You can put edges `myworkflow.putEdge(..)`, add more nodes `myworkflow.addNode(..)` and override the start node `myworkflow.startNode(..)` at Just-in-time after workflow runs. This feature allows you to modify the workflow behavior dynamically during execution.
- **Workflow visualization**: You can generate the workflow image at definition time and at runtime. This feature allows you to visualize the transitions computed of your app workflow. Both kinds of images can be generated in a given path `File` or as `BufferedImage` to be displayed in a java _notebook_. Also, you can use `StyleGraph.SKETCHY` as `StyleAttribute` to generate workflow images with [excalidraw](https://github.com/excalidraw/excalidraw) style. This style is supported in `Graphviz` implementation only.

### v0.2.0 Features
- **Graph-core**: The engine supports create `Nodes`, `Conditional Nodes`, `Edges`, and workflows as a graph. This feature allows you to define custom workflows with multiple `Transitions` between nodes such as one-way, round trip and recursive. 
- **Run workflow**: jAI Workflow supports synchronized `workflow.run()` and streaming `worflow.runStream()` runs the outputs as they are produced by each node. This last feature allows for real-time processing and response in your application.
- **Integration**: [LangChain4j](https://docs.langchain4j.dev/) integration, enabling you to define custom workflows using all the features that LangChain4j offers. This integration provides a comprehensive toolset for building advanced AI applications to integrate with multiple LLM providers and models.
- **Visualization**: The engine supports the generation of workflow images. This feature allows you to visualize the flow computed of your app workflow. By Default it uses `Graphhviz` lib to generate the image, but you implement your own image generator on `GraphImageGenerator.java` interface.
### Q1 2025 Features
- **Graph-Core**:
  - [x] Split Nodes
  - [x] Merge Nodes
  - [x] Parallel transitions (except runtime)
  - Human-in-the-loop
- **Modular (Group of nodes)**:
  - Module
  - Remote Module
- **Integration**:
  - Model Context Protocol (MCP) integration as server and client.
  - Define remote module as MCP server.
- **API**:
  - Publish workflow as API (SSE for streaming runs and REST for sync runs).
### 🗺️ Future Features
- **Deployment Model**:
  - Dockerize workflow
  - Kubernetes deployment
  - Cloud deployment
- **Observability**:
  - OpenTelemetry integration (metrics and traces).
  - Debugging mode logging structure.
- **Playground**:
  - Web-based playground to add, test and run jAI workflows APIs.
  - Chatbot Q&A viewer.
  - Graph tracing visualization for debugging
  - 
  ![jai-workflow-playground-prototype](docs/jai-workflow-playground.gif)
  > _This is a prototype, the final version will be available soon. Open an issue if you want to share your ideas or contribute to this feature._

## Reference Architectures
jAI Workflow is designed with a modular architecture, enabling you to define custom workflows, modules, or agents to build RAG systems as LEGO-like. A module can be decoupled and integrated into any other workflow.

### Standalone Architecture
![Standalone Architecture](docs/jai-standalone-architecture.png)

### Distributed Architecture
![Desired Architecture](docs/jai-distributed-architecture.png)

> 📖 Full documentation and examples will be available soon and regarding new features are planned for the next releases.

## 💡How to use in your Java project
In **jAI Workflow**, the notion of state plays a pivotal role. Every execution of the graph initiates a state, which is then transferred among the nodes during their execution. Each node, after its execution, updates this internal state with its own return value. The method by which the graph updates its internal state is determined by user-defined functions.

The simplest way to use jAI Workflow in your project is with the [LangChain4j](https://docs.langchain4j.dev) integration because enables you to define custom workflows using all the features that LangChain4j offers. This integration could provide a comprehensive toolset for building advanced AI applications:
```xml
<dependency>
  <groupId>io.github.czelabueno</groupId>
  <artifactId>jai-workflow-langchain4j</artifactId>
  <version>0.3.0</version> <!--Change to the latest version-->
</dependency>
```

If you would want to use jAI workflow without LangChain4j or with other framework, add the following dependency to your `pom.xml` file:
```xml
<dependency>
  <groupId>io.github.czelabueno</groupId>
  <artifactId>jai-workflow-core</artifactId>
  <version>0.3.0</version> <!--Change to the latest version-->
</dependency>
```
### Example with langchain4j
Define a stateful bean with fields that will be used to store the state of the workflow:
```java
// Define a stateful bean
public class MyStatefulBean extends AbstractStatefulBean {
  private List<String> relevantDocuments;
  private String webSearchResponse;
  // other fields you need
}
```
Define functions that determines statefulBean state. To simplify this, you can use a java class with static methods:
```java
public class MyStatefulBeanFunctions {
  public static MyStatefulBean searchWeb(MyStatefulBean statefulBean) {
    // This is a simple example, you can use LangChain4j to search the web using any WebSearchEngine.
    statefulBean.webSearchResponse = "Web search response";
    return statefulBean;
  };
  public static MyStatefulBean extractRelevantDocuments(MyStatefulBean statefulBean, String... uris) {
    // This is a simple example, you can use LangChain4j to extract relevant content of the URIs using any RAG pattern.  
    statefulBean.relevantDocuments = Arrays.asList("Relevant Content 1", "Relevant Content 2");
    return statefulBean;
  };
  public static UserMessage generateUserMessageUsingPrompt(MyStatefulBean statefulBean) {
    return UserMessage.from(answerPrompt(statefulBean).text());
  }
  private static Prompt answerPrompt(MyStatefulBean statefulBean) {
    String question = statefulBean.getQuestion();
    String context = String.join("\n\n", statefulBean.getRelevantDocuments());
    MyStructuredPrompt generateAnswerPrompt = new MyStructuredPrompt(question, context);
    return StructuredPromptProcessor.toPrompt(generateAnswerPrompt);
  }
}
```
Create a simple workflow with 3 nodes and conditional edges:
```java
public class Example {
  public static void main(String[] args) {
    
    MyStatefulBean myStatefulBean = new MyStatefulBean();
    String[] documents = new String[]{
            "https://lilianweng.github.io/posts/2023-06-23-agent/",
            "https://lilianweng.github.io/posts/2023-03-15-prompt-engineering/"
    };
    
    StreamingChatLanguageModel streamingModel = MistralAiStreamingChatModel.builder()
            .apiKey(System.getenv("MISTRAL_AI_API_KEY"))
            .modelName(MistralAiChatModelName.MISTRAL_LARGE_LATEST)
            .temperature(0.0)
            .build();
    
    // Create the nodes and associate them with the functions to be used during execution.
    Node<MyStatefulBean, MyStatefulBean> retrieveNode = Node.from(
            "Retrieve Node", 
            obj -> MyStatefulBeanFunctions.extractRelevantDocuments(obj, documents));
    Node<MyStatefulBean, MyStatefulBean> webSearchNode = Node.from(
            "Web Searching Node", 
            obj -> MyStatefulBeanFunctions.searchWeb(obj));
    StreamingNode<MyStatefulBean> generateAnswerNode = StreamingNode.from(
            "Generation Node",
            obj -> MyStatefulBeanFunctions.generateUserMessageUsingPrompt(obj),
            streamingModel);

    // Create workflow with the nodes and transitions.
    DefaultJAiWorkflow<MyStatefulBean> workflow = new DefaultJAiWorkflow<MyStatefulBean>(
            myStatefulBean,
            List.of(Transition.from(retrieveNode, webSearchNode),
                    Transition.from(webSearchNode, Conditional.eval("webSearchResponse",
                            obj -> obj.webSearchResponse != null ? generateAnswerNode : retrieveNode,
                            List.of(retrieveNode, generateAnswerNode))),
                    Transition.from(generateAnswerNode, WorkflowStateName.END)),
            retrieveNode, // Start node
            true // Run in streaming mode
    );
    // Generate workflow image at definition time
    workflow.getWorkflowImage("image/my-workflow.svg");
    // Start conversation with the workflow in streaming mode
    String question = "Summarizes the importance of building agents with LLMs";
    Flux<String> tokens = workflow.answerStream(question);
    tokens.subscribe(System.out::println);
    
    // Generate workflow image at runtime
    workflow.getWorkflowImage("image/my-computed-workflow.svg");
    // Print all computed transitions
    String transitions = workflow.prettyTransitions();
    System.out.println("Transitions: \n");
    System.out.println(transitions);
  }
}
```
Now you can check the output of the workflow execution.

```shell
STARTING workflow in stream mode..
Processing node: Retrieve Node
Retrieve Node: processed
Processing node: Web Searching Node
Web Searching Node: processed
Processing node: Retrieve Node
Retrieve Node: processed
Processing node: Web Searching Node
Web Searching Node: processed
Processing node: Generation Node
Generation Node: processed
Reached END state
```
The LLM answer will be printed by tokens in the console:
```shell
Building 
agen
ts with 
LLMs 
is 
important 
for three 
key reasons. 
Firstly, 
LLMs serve as 
a powerful 
general problem 
solver, 
extending 
their capabilities 
beyond 
just 
generating text. 
Secondly, they 
act as 
the brain
 of an 
autonomous 
agent system,
 enabling tasks 
like planning 
and task 
decomposition. 
Lastly, 
proof-of-concept 
demos like 
AutoGPT 
and 
BabyAGI 
showcase the
 potential of 
LLM-powered 
agents in 
handling
 complex 
tasks
  efficiently.
```

You can print all computed transitions:

```shell
[_start_ -> retrieveNode {Order: 1, ComputedAt: 2025-03-11T01:08:05.839200, Payload: Node1: function proceed }]
[retrieveNode -> webSearchNode {Order: 2, ComputedAt: 2025-03-11T01:08:05.839200, Payload: Node2: function proceed }]
[webSearchNode -> webSearchResponse? {Order: 3, ComputedAt: 2025-03-11T01:08:05.839255, Payload: Node3: function proceed }]
[webSearchResponse? -> generateAnswerNode {Order: 4, ComputedAt: 2025-03-11T01:08:05.839294, Payload: node4 }]
[generateAnswerNode -> _end_ {Order: 5, ComputedAt: 2025-03-11T01:08:05.839353, Payload: Node4: function proceed }]
```
You can generate a workflow image with all computed transitions:
```shell
> image/
> ├── my-workflow.svg
```
![Workflow Image](jai-workflow-core/image/my-workflow-beauty.svg)

```shell
> image/
> ├── my-computed-workflow.svg
```
![Workflow Image](jai-workflow-core/image/my-computed-workflow-beauty.svg)

Check the full example in the [jai-workflow-langchain4j tests](https://github.com/czelabueno/jai-workflow/blob/main/jai-workflow-langchain4j/src/test/java/com/github/czelabueno/jai/workflow/langchain4j/JAiWorkflowIT.java)

## LLM examples
You can check all examples in the [jai-workflow-langchain4j-examples](https://github.com/czelabueno/jai-workflow-langchain4j-examples) repository where show you how-to implement multiple RAG patterns, agent architectures and AI papers using LangChain4j and jAI Workflow. 

> Please note that examples can be modified and more examples will be added over time.

### MoA
- **Mixture-of-Agents (MoA)**:
  - Java example: [`langchain4j-moa`](https://github.com/czelabueno/langchain4j-workflow-examples/tree/main/langchain4j-moa)
  - Based on Paper: https://arxiv.org/pdf/2406.04692

### RAG
- **Corrective RAG (CRAG)**:
  - Java example: [`langchain4j-corrective-rag`](https://github.com/czelabueno/langchain4j-workflow-examples/tree/main/langchain4j-corrective-rag)
  - Based on Paper: https://arxiv.org/pdf/2401.15884
- **Adaptive RAG**:
  - Java example: _Very soon_
  - Based on Paper: https://arxiv.org/pdf/2403.14403
- **Self RAG**:
  - Java example: _Very soon_
  - Based on Paper: https://arxiv.org/pdf/2310.11511
- **Modular RAG**:
  - Java example: _Very soon_
  - Based on Paper: https://arxiv.org/pdf/2312.10997v1

### Agent Architectures
- **Multi-agent Collaboration**:
  - Java example: _Very soon_
  - Based on Paper: https://arxiv.org/pdf/2308.08155
- **Agent Supervisor**:
  - Java example: _Very soon_
  - Based on Paper: https://arxiv.org/pdf/2308.08155
- **Planning Agents**:
  - Java example: _Very soon_
  - Based on Paper: https://arxiv.org/pdf/2305.04091

## ❓ FAQ

### What is jAI Workflow?

jAI Workflow is a **Java library** for building stateful, graph-based agentic workflows. It allows you to design complex AI applications like RAG systems and multi-agent architectures as directed graphs with nodes, transitions, and conditional branching — all in pure Java code.

### How does jAI Workflow differ from other workflow frameworks?

Unlike general workflow engines, jAI Workflow is **AI-native** and **stateful**:
- **Configuration-as-Code**: Workflows are defined in Java, not JSON/YAML
- **Stateful POJO**: Your custom state objects (beans) persist through workflow execution
- **Graph-Based**: Supports one-way, round-trip, loops, recursion, and parallel execution
- **Streaming**: Real-time output as nodes produce results
- **AI Ecosystem**: Integrates with LangChain4j and can extend to other AI frameworks

### What Java version is required?

jAI Workflow requires **Java 11+**. It uses modern Java features like records, streams, and reactive programming (Project Reactor) for streaming workflows.

### How do I add jAI Workflow to my project?

Add the Maven dependency:

```xml
<dependency>
    <groupId>io.github.czelabueno</groupId>
    <artifactId>jai-workflow-core</artifactId>
    <version>0.3.0</version>
</dependency>
```

For LangChain4j integration, also add:

```xml
<dependency>
    <groupId>io.github.czelabueno</groupId>
    <artifactId>jai-workflow-langchain4j</artifactId>
    <version>0.3.0</version>
</dependency>
```

### What LLM providers are supported?

Through LangChain4j integration, jAI Workflow supports:
- **OpenAI**: GPT-4, GPT-3.5-turbo
- **Anthropic**: Claude models
- **Azure OpenAI**: Enterprise deployments
- **Google**: Gemini models
- **Local Models**: Ollama, LM Studio, etc.

### Can I use jAI Workflow without LangChain4j?

Yes! The core `jai-workflow-core` library is independent and can be used with any LLM provider. Simply implement your own `Node` functions that call your preferred LLM API.

### What workflow patterns are supported?

jAI Workflow supports multiple patterns from the Modular RAG AI paper:
- **One-way**: Sequential node execution
- **Parallelization**: Multiple nodes execute concurrently (definition/build time)
- **Routing**: Conditional branching based on state
- **Branching**: Multiple paths from a decision node
- **Conditional**: Dynamic evaluation of which node to execute

### How do I visualize my workflow?

jAI Workflow can generate workflow images:

```java
// Generate at definition time
workflow.generateImage("workflow-definition.svg");

// Generate after execution (shows computed transitions)
workflow.generateComputedImage("workflow-execution.svg");
```

Uses Graphviz by default. Supports `StyleGraph.SKETCHY` for Excalidraw-style diagrams.

### What is a "Computed Transition"?

A `ComputedTransition` records each step during workflow execution:
- Source and target nodes
- Execution order and timestamp
- Input/output payloads

Use this for debugging, observability, and auditing workflow behavior.

### Can I modify a workflow at runtime?

Yes! jAI Workflow supports **JIT modification**:
- `workflow.putEdge(..)` — Add new transitions
- `workflow.addNode(..)` — Add new nodes
- `workflow.startNode(..)` — Change the start node

This allows dynamic behavior changes during execution.

### How do I implement Human-in-the-loop?

Use conditional nodes that pause execution and wait for human input:

```java
Node reviewNode = Node.from("Review", state -> {
    // Wait for human approval
    return state.isApproved() ? approvedNode : rejectedNode;
});
```

### What is a Module?

A `Module` is a reusable group of nodes. Define complex sub-workflows once and reuse them across different applications. Modules promote code reuse and maintainability.

### Can I deploy workflows as APIs?

Yes! jAI Workflow can be published as a REST API using Spring Boot or any Java web framework. Expose `workflow.run(input)` as an endpoint for remote execution.

### How do I debug workflow issues?

- Use `ComputedTransition` list to trace execution
- Generate computed workflow images to visualize paths taken
- Check node input/output payloads in each transition
- Enable logging in your node functions

### Where can I find examples?

See the [jai-workflow-langchain4j-examples](https://github.com/czelabueno/langchain4j-workflow-examples) repository:
- **MoA**: Mixture-of-Agents implementation
- **Corrective RAG**: Error-correcting retrieval workflow
- **Multi-agent Collaboration**: Agent team orchestration (coming soon)

### How do I contribute?

Open an issue or pull request on GitHub. The project welcomes new ideas, bug fixes, and feature implementations. See [Contribute & feedback](#-contribute--feedback) section.

---

## 💬 Contribute & feedback
If you have any feedback, suggestions, or want to contribute, please feel free to open an issue or a pull request. We are open to new ideas and suggestions.
Help us to maturity this project and make it more useful for the java community.

## 🧑🏻‍💻 Authors
- Carlos Zela [@c_zela](https://x.com/c_zela) [czelabueno](https://linkedin.com/in/czelabueno)

<style>
  details {
    margin: 1em 0;
    padding: 0.5em;
    border: 1px solid #ddd;
    border-radius: 5px;
  }
  summary {
    font-weight: bold;
    cursor: pointer;
  }
  summary:hover {
    color: #0073e6;
  }
</style>
