
## LCGraph example

```python
def create_react_agent(
    model: Union[str, LanguageModelLike],
    tools: Union[Sequence[BaseTool], ToolNode],
    *,
    prompt: Optional[Prompt] = None,
    response_format: Optional[
        Union[StructuredResponseSchema, tuple[str, StructuredResponseSchema]]
    ] = None,
    state_schema: Optional[StateSchemaType] = None,
    config_schema: Optional[Type[Any]] = None,
    checkpointer: Optional[Checkpointer] = None,
    store: Optional[BaseStore] = None,
    interrupt_before: Optional[list[str]] = None,
    interrupt_after: Optional[list[str]] = None,
    debug: bool = False,
    version: Literal["v1", "v2"] = "v1",
    name: Optional[str] = None,
) -> CompiledGraph:
    """Creates a graph that works with a chat model that utilizes tool calling.

```

## OpenAI Agents SDK

```python
@function_tool
def get_weather(city: str) -> str:
    return f"The weather in {city} is sunny"

@dataclass
class UserContext:
    uid: str
    is_pro_user: bool

    async def fetch_purchases() -> list[Purchase]:
        return ...

class CalendarEvent(BaseModel):
    name: str
    date: str
    participants: list[str]
    
    
# Agents    
booking_agent = Agent(...)
refund_agent = Agent(...)

agent = Agent[UserContext](
    name="Haiku agent",
    instructions="Always respond in haiku form",
    model="o3-mini",
    tools=[get_weather],
    output_type=CalendarEvent,
    handoffs=[booking_agent, refund_agent],
)

Tool agentTool = agent.as_tool()
```
