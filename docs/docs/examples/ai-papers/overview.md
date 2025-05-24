The flexibility of jAI Workflow enables you to create custom workflows for various AI applications, including agentic RAG-based approaches. The library is designed to be modular and reusable, allowing you to build sophisticated AI applications based on AI papers.

The following section provides an overview of the jAI Workflow library, including its features, architecture, and usage for complex examples. The examples demonstrate how to implement various agentic patterns using jAI Workflow.

- **[Mixture of Agents](mixture-of-agents.md)**: The Mixture-of-Agents methodology leverages multiple models to boost performance, capitalizing on the collaborative nature of Large Language Models (LLMs).
- **[Corrective RAG](corrective-rag.md)**: CRAG is a unique strategy for RAG that integrates self-reflection and self-grading on retrieved documents. It's a process that's as dynamic as it is intelligent, taking some steps to ensure the most relevant information is used for final answer generation.
- **[Modular RAG](modular-rag.md)**: Modular RAG is a new approach to RAG that emphasizes the importance of modularity and flexibility in the retrieval process. It allows for the integration of multiple retrieval methods and models, enabling a more dynamic and adaptable system.

!!! example "More examples"

    More examples will be added over time, showcasing the flexibility and power of jAI Workflow in building advanced AI applications. The examples will cover various use cases, including agentic RAG-based approaches, agent architectures, and other innovative AI methodologies. 

## jAI usage examples
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
