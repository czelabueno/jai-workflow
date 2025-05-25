
# ![logo](docs/docs/static/logo-180x180.png) jAI Workflow ☕: Build programmatically custom agentic workflows, AI Agents, Agentic RAG systems for java.
[![Build Status](https://github.com/czelabueno/langchain4j-workflow/actions/workflows/ci.yaml/badge.svg)](https://github.com/czelabueno/langchain4j-workflow/actions/workflows/ci.yaml)
[![Version](https://img.shields.io/maven-central/v/io.github.czelabueno/jai-workflow-parent?logo=apachemaven)](https://search.maven.org/#search|gav|1|g:"io.github.czelabueno"%20AND%20a:"jai-workflow-core")
[![Docs](https://img.shields.io/badge/docs-latest-blue)](https://czelabueno.github.io/jai-workflow/)
[![Open Issues](https://img.shields.io/github/issues-raw/czelabueno/jai-workflow)](https://github.com/czelabueno/jai-workflow/issues)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

An open-source Java library to build, package, integrate, orchestrate and monitor AI agentic systems for java developers 💡


<center><a href="https://czelabueno.github.io/jai-workflow/">📚 https://czelabueno.github.io/jai-workflow</a></center>

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

## 🚀 Features
### Q1 2025 Features
- **Agent**:
  - [x] Contextual Agent
  - [x] Stateful Agent
  - [x] Agnostic Model (LLMs)
  - [x] Tools
  - [x] Structured output
  - [x] Streaming
- **Workflow**:
  - [x] Generic Nodes
  - [x] Conditional Nodes
  - [x] Split Nodes
  - [x] Merge Nodes
  - [ ] User Nodes (Human-in-the-loop)
  - [x] Conditional transitions
  - [x] Sequential transitions
  - [x] Parallel transitions
  - [ ] Loop transitions
- **Modular (Reusability)**:
  - [X] Module
  - [ ] Remote Module
- **Integration**:
  - [x] LangChain4j
  - [ ] Spring AI
  - [ ] Model Context Protocol (MCP) integration as server and client.
  - [ ] Define remote module as MCP server.
- **Observability**:
  - [ ] OpenTelemetry integration (metrics and traces).
  - [X] Debugging mode logging structure.
- **API**:
  - [ ] Publish workflow as Functional API (SSE for streaming runs and REST for sync runs).
  
### 🗺️ Future Experimental Features
- **Deployment Model**:
  - Dockerize workflow
  - Kubernetes deployment
  - Cloud deployment
- **Playground**:
  - Web-based playground to add, test and run jAI workflows APIs.
  - Chatbot Q&A viewer.
  - Graph tracing visualization for debugging
  - 
  ![jai-workflow-playground-prototype](docs/jai-workflow-playground.gif)
  > _This is a prototype, the final version will be available soon. Open an issue if you want to share your ideas or contribute to this feature._
  

## Get started

Add the dependency to your project:
```xml
<dependency>
    <groupId>io.github.czelabueno</groupId>
    <artifactId>jai-workflow-core</artifactId>
    <version>${jai.workflow.version}</version>
</dependency>
```

Then, create your first agentic workflow:
```java
// TODO: Add a simple example of a workflow
```

For more details, check the [documentation](https://czelabueno.github.io/jai-workflow/).

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
  img[alt=logo] { width: 55px; }
</style>
