
``` mermaid
stateDiagram-v2
    [*] --> Start
    Start --> Agent
    Agent --> Tools : continue
    Tools --> Agent
    Agent --> End : end
    End --> [*]

    classDef startClass fill:#ffdfba;
    classDef endClass fill:#baffc9;
    classDef otherClass fill:#fad7de;

    class Start startClass
    class End endClass
    class Agent,Tools otherClass
```
The agent then returns the full list of messages as a dictionary containing the key "messages".

``` mermaid
    sequenceDiagram
        participant U as User
        participant A as Agent (LLM)
        participant T as Tools
        U->>A: Initial input
        Note over A: Messages modifier + LLM
        loop while tool_calls present
            A->>T: Execute tools
            T-->>A: ToolMessage for each tool_calls
        end
        A->>U: Return final state
```
