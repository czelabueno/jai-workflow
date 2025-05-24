## jAI Workflow quick-start
In **jAI Workflow**, the notion of state plays a pivotal role. Every execution of the workflow initiates a state, which is then transferred among the nodes during their execution. Each node, after its execution, updates this internal state with its own return value. The method by which the workflow updates its internal state is determined by user-defined functions.

The simplest way to use jAI Workflow in your project is with the [LangChain4j](https://docs.langchain4j.dev) integration because enables you to define custom workflows using all the features that LangChain4j offers. This integration could provide a comprehensive toolset for building advanced AI applications:

## Prerequisites
Before you start, ensure you have the following:

- Java 17 or higher.
- Maven 3.8 or higher OR Gradle 8.0 or higher.
- A [Mistral AI](https://console.mistral.ai/user/api-keys) API key. 

## 1. Add dependencies
To use jAI Workflow with LangChain4j, add the following dependency:

=== "Maven"
    ```xml title="pom.xml"
    <dependency>
      <groupId>io.github.czelabueno</groupId>
      <artifactId>jai-workflow-langchain4j</artifactId>
      <version>0.3.0</version>
    </dependency>
    ```
=== "Gradle"
    ```groovy title="build.gradle"
    dependencies {
        implementation 'io.github.czelabueno:jai-workflow-langchain4j:0.3.0' //Change to the latest version
    }
    ```

If you would want to use jAI workflow without LangChain4j or with other framework, add the following dependency:

=== "Maven"
    ```xml title="pom.xml"
    <dependency>
      <groupId>io.github.czelabueno</groupId>
      <artifactId>jai-workflow-core</artifactId>
      <version>0.3.0</version> <!--Change to the latest version-->
    </dependency>
    ```

=== "Gradle"
    ```groovy title="build.gradle"
    dependencies {
        implementation 'io.github.czelabueno:jai-workflow-core:0.3.0'
    }
    ```

## 2. Create your stateful bean
Define a stateful bean with fields that will be used to store the state of the workflow:
```java
// Define a stateful bean
public class MyStatefulBean extends AbstractStatefulBean {
  private List<String> relevantDocuments;
  private String webSearchResponse;
  // other fields you need
}
```

## 3. Define your functions
Define functions that determines statefulBean state. To simplify this, you can use a java class with static methods:
```java
public class MyStatefulBeanFunctions {
  public static MyStatefulBean searchWeb(MyStatefulBean statefulBean) {
    // This is a simple example, you can use LangChain4j to search the web using any WebSearchEngine.
    statefulBean.webSearchResponse = "Web search response";
    return statefulBean;
  };
  public static MyStatefulBean extractRelevantDocuments(MyStatefulBean statefulBean, String... uris) {
    // This is a simple example, you can use LangChain4j to extract relevant content of the URIs using any Document Loader.  
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

## 4. Define your workflow
Create a simple workflow with 3 nodes, 1 conditional node and multiple transitions:
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
    // Start conversation with the workflow in streaming mode
    String question = "Summarizes the importance of building agents with LLMs";
    Flux<String> tokens = workflow.answerStream(question);
    tokens.subscribe(System.out::println);
  }
}
```

## 5. Workflow visualization
If you are interested to display the workflow image, you can do it in two different ways:

* **Definition time**: You can generate the workflow image at definition time, before running the workflow. This feature allows you to visualize the flow of your application before executing it to be sure that your workflow will be built properly.
* **Runtime**: You can generate the workflow image at runtime, after running the workflow. This feature allows you to visualize the flow of your application after executing it.

=== "Definition time"

    After building the workflow, you can generate the workflow image at definition time:

    ``` java {hl_lines="13 15"}
    // Create workflow with the nodes and transitions.
    var workflow = new DefaultJAiWorkflow<MyStatefulBean>(
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
    // Generate workflow image as BufferedImage
    BufferedImage image = workflow.getWorkflowImage();
    ```
    Located in the `image` folder:

    ```shell
    > image/
    > ├── my-workflow.svg
    ```
    Displayed:    
    ![Workflow Image](https://raw.githubusercontent.com/czelabueno/jai-workflow/refs/heads/main/jai-workflow-core/image/my-workflow-beauty.svg)

=== "Runtime"

    After running the workflow, you can generate the workflow image at runtime to check which nodes were executed:

    ``` java {hl_lines="14 18 20"}
    // Create workflow with the nodes and transitions.
    var workflow = new DefaultJAiWorkflow<MyStatefulBean>(
            myStatefulBean,
            List.of(Transition.from(retrieveNode, webSearchNode),
                    Transition.from(webSearchNode, Conditional.eval("webSearchResponse",
                            obj -> obj.webSearchResponse != null ? generateAnswerNode : retrieveNode,
                            List.of(retrieveNode, generateAnswerNode))),
                    Transition.from(generateAnswerNode, WorkflowStateName.END)),
            retrieveNode, // Start node
            true // Run in streaming mode
    );
    // Start conversation with the workflow in streaming mode
    String question = "Summarizes the importance of building agents with LLMs";
    Flux<String> tokens = workflow.answerStream(question);
    tokens.subscribe(System.out::println);
    
    // Generate workflow image at runtime
    workflow.getWorkflowImage("image/my-computed-workflow.svg"); // (1)!
    // Generate workflow image as BufferedImage
    BufferedImage image = workflow.getWorkflowImage(); // (2)!
    ```

    1. The method `getWorkflowImage("path")` is the same that Definition time, but notice that is called after the workflow runs invoking to the method `.answerStream(question)`. This method will generate the image with all computed transitions and nodes that were executed during the workflow run.
    2. The method `getWorkflowImage()` is the same that Definition time, but notice that is called after the workflow runs invoking to the method `.answerStream(question)`. This method will generate the image with all computed transitions and nodes that were executed during the workflow run.

    Located in the `image` folder:

    ```shell
    > image/
    > ├── my-computed-workflow.svg
    ```
    Displayed:

    ![Workflow Image](https://raw.githubusercontent.com/czelabueno/jai-workflow/refs/heads/main/jai-workflow-core/image/my-computed-workflow-beauty.svg)

## 6. Print all computed transitions
You can print all computed transitions once the workflow has run to see all the execution details, it's like having a debug mode:

``` java {hl_lines="16 18"}
// Create workflow with the nodes and transitions.
var workflow = new DefaultJAiWorkflow<MyStatefulBean>(
        myStatefulBean,
        List.of(Transition.from(retrieveNode, webSearchNode),
                Transition.from(webSearchNode, Conditional.eval("webSearchResponse",
                        obj -> obj.webSearchResponse != null ? generateAnswerNode : retrieveNode,
                        List.of(retrieveNode, generateAnswerNode))),
                Transition.from(generateAnswerNode, WorkflowStateName.END)),
        retrieveNode, // Start node
        true // Run in streaming mode
);
// Start conversation with the workflow in streaming mode
String question = "Summarizes the importance of building agents with LLMs";
Flux<String> tokens = workflow.answerStream(question);
// Print all computed transitions
String transitions = workflow.prettyTransitions();
System.out.println("Transitions: \n");
System.out.println(transitions);
```
Console:

```shell
[_start_ -> retrieveNode {Order: 1, ComputedAt: 2025-03-11T01:08:05.839200, Payload: Node1: function proceed }]
[retrieveNode -> webSearchNode {Order: 2, ComputedAt: 2025-03-11T01:08:05.839200, Payload: Node2: function proceed }]
[webSearchNode -> webSearchResponse? {Order: 3, ComputedAt: 2025-03-11T01:08:05.839255, Payload: Node3: function proceed }]
[webSearchResponse? -> generateAnswerNode {Order: 4, ComputedAt: 2025-03-11T01:08:05.839294, Payload: node4 }]
[generateAnswerNode -> _end_ {Order: 5, ComputedAt: 2025-03-11T01:08:05.839353, Payload: Node4: function proceed }]
```
## Next Steps
- [Deploy your example locally](deployment/local.md) 
- [Build your first agent](../guides/agents/first-agent.md)
- [Build your first workflow](../guides/workflows/first-workflow.md)
