package io.github.czelabueno.jai.workflow;

import io.github.czelabueno.jai.workflow.graph.Format;
import io.github.czelabueno.jai.workflow.graph.graphviz.GraphvizImageGenerator;
import io.github.czelabueno.jai.workflow.graph.graphviz.Orientation;
import io.github.czelabueno.jai.workflow.graph.graphviz.StyleGraph;
import io.github.czelabueno.jai.workflow.node.Conditional;
import io.github.czelabueno.jai.workflow.node.Node;
import io.github.czelabueno.jai.workflow.transition.ComputedTransition;
import io.github.czelabueno.jai.workflow.transition.Transition;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StateWorkflowTest {

    // validated
    class MyStatefulBean {
        int value = 0;

        @Override
        public String toString() {
            return "MyStatefulBean{" +
                    "value=" + value +
                    '}';
        }
    }

    private MyStatefulBean myStatefulBean;
    private StateWorkflow<MyStatefulBean> myWorkflow;
    private Node<MyStatefulBean, String> node1;
    private Node<MyStatefulBean, String> node2;
    private Node<MyStatefulBean, String> node3;
    private Node<MyStatefulBean, String> node4;

    @BeforeEach
    void setUp() {
        myStatefulBean = new MyStatefulBean();
        // Define functions for nodes
        Function<MyStatefulBean, String> node1Func = obj -> {
            obj.value +=1;
            System.out.println("Node 1: [" + obj.value + "]");
            return "Node1: processed function";
        };
        Function<MyStatefulBean, String> node2Func = obj -> {
            obj.value +=2;
            System.out.println("Node 2: [" + obj.value + "]");
            return "Node2: processed function";
        };
        Function<MyStatefulBean, String> node3Func = obj -> {
            obj.value +=3;
            System.out.println("Node 3: [" + obj.value + "]");
            return "Node3: processed function";
        };
        Function<MyStatefulBean, String> node4Func = obj -> {
            obj.value +=4;
            System.out.println("Node 4: [" + obj.value + "]");
            return "Node4: processed function";
        };

        node1 = Node.from("node1", node1Func);
        node2 = Node.from("node2", node2Func);
        node3 = Node.from("node3", node3Func);
        node4 = Node.from("node4", node4Func);

        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, node2), Transition.from(node2, node3))
                .addNodes(node1, node2, node3)
                .build();
    }

    // 1) Workflow definition test cases
    @Test
    void should_construct_workflow_with_minimum_values_required() {
        // when
        MyStatefulBean myStatefulBean2 = new MyStatefulBean();
        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean2)
                .addEdges(Transition.from(node1, node2), Transition.from(node2, node3), Transition.from(node3, node4))
                .build();
        // then
        Node lastNodeDefined = myWorkflow.getLastNode();
        assertEquals(node4, lastNodeDefined);
    }

    @Test
    void should_throw_illegalArgumentException_without_statefulBean() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> DefaultStateWorkflow.<MyStatefulBean>builder()
                        .addEdges(Transition.from(node1, node2), Transition.from(node2, node3))
                        .build())
                .withMessage("Stateful bean cannot be null");
    }

    @Test
    void should_throw_illegalArgumentException_without_edges() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> DefaultStateWorkflow.<MyStatefulBean>builder()
                        .statefulBean(myStatefulBean)
                        .addNodes(node1, node2, node3)
                        .build())
                .withMessage("At least one edged must be added to the workflow");
    }

    @Test
    void should_construct_workflow_with_nodes() {
        // when
        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, node2), Transition.from(node2, node3))
                .addNodes(node1, node2, node3)
                .build();
        // then
        Node lastNodeDefined = myWorkflow.getLastNode();
        assertEquals(node3, lastNodeDefined);
    }

    @Test
    void should_construct_workflow_with_graphImageGenerator() {
        // when
        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, node2), Transition.from(node2, node3))
                .graphImageGenerator(GraphvizImageGenerator.builder().build())
                .build();
        // then
        Node lastNodeDefined = myWorkflow.getLastNode();
        assertEquals(node3, lastNodeDefined);
    }

    @Test
    void should_throw_illegalArgumentException_if_a_parallel_node_is_a_split_node() {
        Node node5 = Node.from("node5", obj -> "node5");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> DefaultStateWorkflow.<MyStatefulBean>builder()
                        .statefulBean(myStatefulBean)
                        .addEdges(Transition.from(node1, node2), Transition.from(node1, node3), Transition.from(node2, node4), Transition.from(node2, node5))
                        .build())
                .withMessage("A parallel state 'node2' cannot be a split state in the same flow");
    }

    @Test
    void should_throw_illegalArgumentException_if_parallel_next_node_is_not_a_merge_node_or_parallel_node() {
        Node node5 = Node.from("node5", obj -> "node5");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> DefaultStateWorkflow.<MyStatefulBean>builder()
                        .statefulBean(myStatefulBean)
                        .addEdges(Transition.from(node1, node2), Transition.from(node1, node3), Transition.from(node1, node4), Transition.from(node2, node5), Transition.from(node3, node5))
                        .build())
                .withMessage("The merge state 'node5' must have the same number of input transitions as the number of output transitions from the split node 'node1'");
    }

    @Test
    void should_throw_illegalArgumentException_if_parallel_next_node_is_a_WorkflowStateName() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> DefaultStateWorkflow.<MyStatefulBean>builder()
                        .statefulBean(myStatefulBean)
                        .addEdges(Transition.from(node1, node2), Transition.from(node1, node3), Transition.from(node2, WorkflowStateName.END))
                        .build())
                .withMessage("The state node2 labeled as 'Parallel' cannot have a WorkflowStateName '_end_' as an adjacent node");
    }

    // 2) Workflow building test cases
    @Test
    void should_construct_workflow_with_build_arg_start_node() {
        // when
        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, node2), Transition.from(node2, node3))
                .build(node1);
        // then
        Node lastNodeDefined = myWorkflow.getLastNode();
        assertEquals(node3, lastNodeDefined);
    }

    // 3) Workflow run and put edges and nodes after build test cases
    @Test
    void should_throw_illegalArgumentException_for_inconsistent_start_transition(){
        // then
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> {
                    myWorkflow.putEdge(node1, WorkflowStateName.START);
                    myWorkflow.putEdge(node2, node3);
                    myWorkflow.run();
                })
                .withMessage("Cannot transition to a START state");
    }

    @Test
    void should_add_transitions_and_run_workflow_and_return_statefulbean_modified() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        assertEquals(3, myWorkflow.getComputedTransitions().size()); // start -> node1 -> node2
        assertEquals(6, myStatefulBean.value);
    }

    @Test
    void should_add_transitions_and_run_stream_workflow_and_return_statefulbean_modified() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.startNode(node1);
        myWorkflow.runStream(node -> {
            assertThat(node.graphName()).containsIgnoringCase("node");
            assertThat(node.input()).isNotNull(); // stateful bean must not be null
            assertThat(node.output()).asString().containsIgnoringCase("processed function");
        });
        assertEquals(3, myWorkflow.getComputedTransitions().size()); // start -> node1 -> node2
        assertEquals(6, myStatefulBean.value);
    }

    @Test
    void should_run_stream_workflow_with_split_parallel_merge_nodes() {
        // given
        Node node5 = Node.from("node5", obj -> "node5");
        Node node6 = Node.from("node6", obj -> "node6");
        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, node2), // node1 is a split node
                        Transition.from(node1, node3), // node2 is a parallel node
                        Transition.from(node2, node4), // node3 is a parallel node
                        Transition.from(node3, node5), // node4 is a parallel node
                        Transition.from(node4, node6), // node5 is a parallel node
                        Transition.from(node5, node6)) // node6 is a merge node
//                        Transition.from(node2, node4), // node3 is a parallel node
//                        Transition.from(node3, node4)) // node4 is a parallel node
                .build();
        // when
        myWorkflow.runStream(state -> {
            Node node = (Node) state; // cast to Node
            assertThat(node.getName()).containsIgnoringCase("node");
            if (node.getName().equals("node1")) assertThat(node.hasLabel("Split")).isTrue();
            if (node.getName().equals("node2") || node.getName().equals("node3")
                    || node.getName().equals("node4") || node.getName().equals("node5")) {
                assertThat(node.hasLabel("Parallel")).isTrue();
            }
            if (node.getName().equals("node6")) assertThat(node.hasLabel("Merge")).isTrue();
        });

        // then
        // start -> node1 -> [node2 -> node4, node3 -> node5] -> node6 -> end
        assertEquals(7, myWorkflow.getComputedTransitions().size());
    }

    @Test
    void should_run_workflow_with_modules() {
        // given
        Node node5 = Node.from("node5", obj -> "node5");

        // Create module
        StateWorkflow<MyStatefulBean> module = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Module1")
                .addEdges(Transition.from(node3, node4)) // Nested workflow node to node
                .build()
                .toModule();

        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, node2), // node to node
                        Transition.from(node2, module), // node to module
                        Transition.from(module, node5), // module to node
                        Transition.from(node5, WorkflowStateName.END)) // node to END
                .build();

        // when
        myWorkflow.runStream(node -> System.out.println(node.graphName()));

        // then
        // start -> node1 -> node2 -> module[node3 -> node4] -> node5 -> end
        assertEquals(2, module.getComputedTransitions().size());
        assertEquals(4, myWorkflow.getComputedTransitions().size());
    }

    @Test
    void should_run_workflow_with_modules_and_return_statefulbean_modified() {
        // given
        Node node5 = Node.from("node5", obj -> "node5");

        // Create module
        StateWorkflow<MyStatefulBean> module = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Module1")
                .addEdges(Transition.from(node3, node4)) // Nested workflow node to node
                .build()
                .toModule();

        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, node2), // node to node
                        Transition.from(node2, module), // node to module
                        Transition.from(module, node5), // module to node
                        Transition.from(node5, WorkflowStateName.END)) // node to END
                .build();

        // when
        myWorkflow.run();

        // then
        assertEquals(10, myStatefulBean.value);
    }

    @Test
    void should_run_workflow_with_modules_after_conditional_node() {
        // given
        Node node5 = Node.from("node5", obj -> "node5");

        // Create module
        StateWorkflow<MyStatefulBean> module = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Module1")
                .addEdges(Transition.from(node3, node4)) // Nested workflow node to node
                .build()
                .toModule();


        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, node2), // node to node
                        Transition.from(node2, Conditional.<MyStatefulBean>eval("greater than 6?", obj -> {
                            if (obj.value > 6) {
                                return node5;
                            } else {
                                return module; // expected return module
                            }
                        }, List.of(module, node5))), // conditional node to module or node3
                        Transition.from(module, node5), // module to
                        Transition.from(node5, WorkflowStateName.END)) // node to END
                .build();;

        // when
        myWorkflow.runStream(node -> System.out.println(node.graphName()));

        // then
        // start -> node1 -> node2 -> cond -> module[node3 -> node4] -> node5 -> end
        assertEquals(5, myWorkflow.getComputedTransitions().size());
    }

    @Test
    void should_run_workflow_with_two_modules_conditioned() {
        // given
        Node node5 = Node.from("node5", obj -> "node5");
        Node node6 = Node.from("node6", obj -> "node6");

        // Create module
        StateWorkflow<MyStatefulBean> module1 = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Module1")
                .addEdges(Transition.from(node3, node4)) // Nested workflow node to node
                .build()
                .toModule();

        StateWorkflow<MyStatefulBean> module2 = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Module2")
                .addEdges(Transition.from(node5, node6)) // Nested workflow node to node
                .build()
                .toModule();

        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, Conditional.<MyStatefulBean>eval("greater than 6?", obj -> {
                            if (obj.value > 1) {
                                return module2; // expected return module2
                            } else {
                                return module1; // expected return module1
                            }
                        }, List.of(module1, module2))), // conditional node to module or node3
                        Transition.from(module1, node2), // module to END
                        Transition.from(module2, WorkflowStateName.END),
                        Transition.from(node2, WorkflowStateName.END)) // module to END
                .build();

        // when
        myWorkflow.runStream(node -> System.out.println(node.graphName()));

        // then
        // start -> node1 -> cond -> [module1[node3 -> node4] -> node2] or module2[node5 -> node6] -> end
        assertEquals(4, myWorkflow.getComputedTransitions().size());
    }

    @Test
    void should_run_workflow_with_module_as_first_node_to_parallel_nodes() {
        // given
        Node node5 = Node.from("node5", obj -> "node5");
        Node node6 = Node.from("node6", obj -> "node6");
        // Create module
        StateWorkflow<MyStatefulBean> module1 = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Module1")
                .addEdges(Transition.from(node1, node2)) // Nested workflow node to node
                .build()
                .toModule();

        StateWorkflow<MyStatefulBean> module2 = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Module2")
                .addEdges(Transition.from(node5, node6)) // Nested workflow node to node
                .asModule()
                .build();

        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(module1, node3), // module1 to node3
                        Transition.from(module1, node4), // module1 to node4
                        Transition.from(node3, module2), // node3 to module2
                        Transition.from(node4, module2), // node4 to module2
                        Transition.from(module2, WorkflowStateName.END)) // module2 to END
                .build();

        // when
        myWorkflow.runStream(state -> {
            if (state.equals(module1)) assertThat(state.hasLabel("Split")).isTrue();
            if (state.equals(node3) || state.equals(node4)) {
                assertThat(state.hasLabel("Parallel")).isTrue();
            }
            if (state.equals(module2)) assertThat(state.hasLabel("Merge")).isTrue();
        });

        // then
        // start -> module1[node1 -> node2] -> [node3, node4] -> module2[node5 -> node6] -> end
        assertEquals(5, myWorkflow.getComputedTransitions().size());
    }

    @Test
    void should_run_workflow_with_parallel_modules() {
        // given
        Node node5 = Node.from("node5", obj -> "node5");
        Node node6 = Node.from("node6", obj -> "node6");

        // Create module
        StateWorkflow<MyStatefulBean> module1 = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Module1")
                .addEdges(Transition.from(node2, node3))
                .build()
                .toModule();

        StateWorkflow<MyStatefulBean> module2 = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Module2")
                .addEdges(Transition.from(node4, node5))
                .asModule()
                .build();

        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node1, module1 ), // module1 is parallel
                        Transition.from(node1, module2), // module2 is parallel
                        Transition.from(module1, node6),
                        Transition.from(module2, node6)) // node6 is the merge node
                .build();

        // when
        myWorkflow.runStream(state -> {
            if (state.equals(node1)) assertThat(state.hasLabel("Split")).isTrue();
            if (state.equals(module1)) assertThat(state.hasLabel("Parallel")).isTrue();
            if (state.equals(module2)) assertThat(state.hasLabel("Parallel")).isTrue();
            if (state.equals(node6)) assertThat(state.hasLabel("Merge")).isTrue();
        });

        // then
        // start -> node1 -> [module1, module2] -> node6 -> end
        assertEquals(5, myWorkflow.getComputedTransitions().size());
    }

    @Test
    void should_run_workflow_with_nested_modules() {
        // given
        Node node0 = Node.from("node0", obj -> "node0");
        Node node5 = Node.from("node5", obj -> "node5");

        // Create inner module
        StateWorkflow<MyStatefulBean> weather = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Weather Graph")
                .addEdges(Transition.from(node2, node3))
                .asModule()
                .build();

        StateWorkflow<MyStatefulBean> graph = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .name("Graph")
                .addEdges(Transition.from(node1, Conditional.<MyStatefulBean>eval("graph_router_node",
                                obj -> {
                                        if (obj.value > 6) {
                                            return node4;
                                        } else {
                                            return weather; // expected return node3
                                        }
                                    }, List.of(node4, weather))),
                        Transition.from(node4, node5),
                        Transition.from(weather, node5))
                .build()
                .toModule();



        myWorkflow = DefaultStateWorkflow.<MyStatefulBean>builder()
                .statefulBean(myStatefulBean)
                .addEdges(Transition.from(node0, Conditional.<MyStatefulBean>eval("router_node",
                                obj -> {
                                    if (obj.value < 1) {
                                        return graph; // expected return graph
                                    } else {
                                        return WorkflowStateName.END;
                                    }
                                }, List.of(graph, WorkflowStateName.END))),
                        Transition.from(graph, WorkflowStateName.END))
                .build();

        // when
        myWorkflow.runStream(node -> System.out.println(node.graphName()));

        // then
        // start -> node0 -> cond -> graph[node1 -> cond -> weather[node2 -> node3] OR node4] OR node5 -> end
        assertEquals(3, myWorkflow.getComputedTransitions().size());
    }

    @Test
    void should_run_stream_workflow_with_conditional_node() {
        // given
        myWorkflow.putEdge(node3, Conditional.eval("greater than 6?", obj -> {
            if (obj.value > 6) {
                return node4;
            } else {
                return node2; // expected return node2
            }
        }, List.of(node2, node4)));
        myWorkflow.putEdge(node4, WorkflowStateName.END);
        // when
        myWorkflow.runStream(node -> {
            assertThat(((MyStatefulBean)node.input()).value).isGreaterThanOrEqualTo(1);
            assertThat(node.output()).asString().containsIgnoringCase("processed function");
        });
        // then
        // start -> node1 -> node2 -> node3 -> cond -> node2 -> node3 -> cond-> node4 -> end
        assertEquals(8, myWorkflow.getComputedTransitions().size());
        assertEquals(15, myStatefulBean.value);
    }

    @Test
    void should_start_node2_and_run_workflow_and_return_statefulbean_modified() {
        // given
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, node3);
        // when
        myWorkflow.startNode(node2); // start from node2
        myWorkflow.run();
        // then
        assertEquals(2, myWorkflow.getComputedTransitions().size()); // start -> node2 -> node3
        assertEquals(5, myStatefulBean.value);
    }

    @Test
    void should_run_workflow_with_conditional_node_and_return_statefulbean_modified() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, node3);
        myWorkflow.putEdge(node3, Conditional.eval("greater than 6?", obj -> {
            if (obj.value > 6) {
                return node4;
            } else {
                return node2; // expected return node2
            }
        }, List.of(node2, node4)));
        myWorkflow.putEdge(node4, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        assertEquals(8, myWorkflow.getComputedTransitions().size()); // start -> node1 -> node2 -> node3 -> cond -> node2 -> node3 -> cond-> node4 -> end
        assertEquals(15, myStatefulBean.value);
    }

    @Test
    void should_run_workflow_with_workflow_state_name_end() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        assertEquals(2, myWorkflow.getComputedTransitions().size()); // start -> node1 -> node2 -> end
        assertEquals(3, myStatefulBean.value);
    }

    @Test
    void should_run_workflow_print_pretty_transitions() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        String transitions = ((DefaultStateWorkflow)myWorkflow).prettyTransitions();
        assertThat(3).isEqualTo(myWorkflow.getComputedTransitions().size());
        assertThat(transitions).containsPattern("node\\d+ -> node\\d+");
    }

    @Test
    void should_throw_IllegalStateException_when_run_workflow_with_multiple_start_nodes() {
        Node node5 = Node.from("node5", obj -> "node5");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> DefaultStateWorkflow.<MyStatefulBean>builder()
                        .statefulBean(myStatefulBean)
                        .addEdges(Transition.from(node1, node2), // node1 is a start node
                                Transition.from(node3, node4), // node3 is a start node too
                                Transition.from(node2, node5),
                                Transition.from(node4, node5))
                        .build()
                        .run())
                .withMessageStartingWith("Its not possible to determine the start node, multiple start nodes found:")
                .withMessageContaining("node1")
                .withMessageContaining("node3")
                .withMessageEndingWith("Please specify the start node using the .startNode(Node<T> node) method");
    }

    @Test
    void should_throw_RuntimeException_when_call_computed_method_without_run_workflow() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> myWorkflow.getComputedTransitions())
                .withMessage("Workflow has not been run yet. No transitions computed");
    }

    @Test
    void should_get_workflow_log_entries_of_computed_transitions_after_run() {
        myWorkflow.startNode(node1);
        myWorkflow.run();
        List<ComputedTransition> computedTransitions = myWorkflow.getComputedTransitions();
        System.out.println("Computed Transitions: " + computedTransitions);
        computedTransitions.forEach(ct -> {
            if (ct.getTransition().from().equals(node1)) { // getting computed transition details from node1
                assertThat(ct.getId()).isNotNull();
                assertThat(ct.getTransition().to()).isEqualTo(node2);
                assertThat(ct.getPayload()).isEqualTo("Node1: processed function");
                assertThat(ct.getOrder()).isEqualTo(1);
                assertThat(ct.getComputedAt()).isBefore(LocalDateTime.now());
            }
        });
    }

    // 4) Workflow image generation test cases
    @SneakyThrows(IOException.class)
    @Test
    void should_generate_definition_workflow_image_file_using_path() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        String imagePath = "image/my-workflow-from-test.svg";
        myWorkflow.generateWorkflowImage(imagePath);
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @SneakyThrows(IOException.class)
    @Test
    void should_generate_definition_workflow_image_file_using_path_and_vertical_orientation() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        String imagePath = "image/my-workflow-vertical-from-test.svg";
        myWorkflow.generateWorkflowImage(imagePath, List.of(Orientation.VERTICAL));
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @SneakyThrows(IOException.class)
    @Test
    void should_generate_definition_workflow_image_file_using_path_and_horizontal_orientation() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        String imagePath = "image/my-workflow-horizontal-from-test.svg";
        myWorkflow.generateWorkflowImage(imagePath, List.of(Orientation.HORIZONTAL));
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @SneakyThrows(IOException.class)
    @Test
    void should_generate_definition_workflow_image_file_using_path_and_sketchy_styled() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        String imagePath = "image/my-workflow-sketchy-from-test.svg";
        myWorkflow.generateWorkflowImage(imagePath, List.of(StyleGraph.SKETCHY));
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @SneakyThrows(IOException.class)
    @Test
    void should_generate_definition_workflow_image_PNG_file_using_path_and_sketchy_styled() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        String imagePath = "image/my-workflow-sketchy-from-test.png";
        myWorkflow.generateWorkflowImage(Format.PNG, imagePath, List.of(StyleGraph.SKETCHY));
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @Test
    void should_generate_definition_workflow_buffered_image() {
        // given
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        // when
        BufferedImage image = myWorkflow.generateWorkflowBufferedImage();
        // then
        assertThat(image).isNotNull();
        assertThat(image.getType()).isEqualTo(BufferedImage.TYPE_INT_ARGB);
        assertThat(image.getWidth()).isGreaterThan(0);
        assertThat(image.getHeight()).isGreaterThan(0);
    }

    @SneakyThrows(IOException.class)
    @Test
    void should_generate_computed_workflow_image_file_using_path() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        String imagePath = "image/my-computed-workflow-from-test.svg";
        myWorkflow.generateComputedWorkflowImage(imagePath);
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @SneakyThrows(IOException.class)
    @Test
    void should_generate_computed_workflow_image_file_using_path_and_vertical_orientation() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        String imagePath = "image/my-computed-workflow-vertical-from-test.svg";
        myWorkflow.generateComputedWorkflowImage(imagePath, List.of(Orientation.VERTICAL));
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @SneakyThrows(IOException.class)
    @Test
    void should_generate_computed_workflow_image_file_using_path_and_horizontal_orientation() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        String imagePath = "image/my-computed-workflow-horizontal-from-test.svg";
        myWorkflow.generateComputedWorkflowImage(imagePath, List.of(Orientation.HORIZONTAL));
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @SneakyThrows(IOException.class)
    @Test
    void should_generate_computed_workflow_image_file_using_path_and_sketchy_styled() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        String imagePath = "image/my-computed-workflow-sketchy-from-test.svg";
        myWorkflow.generateComputedWorkflowImage(imagePath, List.of(StyleGraph.SKETCHY));
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @SneakyThrows(IOException.class)
    @Test
    void should_generate_computed_workflow_image_PNG_file_using_path_and_sketchy_styled() {
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        String imagePath = "image/my-computed-workflow-sketchy-from-test.png";
        myWorkflow.generateComputedWorkflowImage(Format.PNG, imagePath, List.of(StyleGraph.SKETCHY));
        Path filePath = Paths.get(imagePath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @Test
    void should_generate_computed_workflow_buffered_image() {
        // given
        myWorkflow.putEdge(node1, node2);
        myWorkflow.putEdge(node2, WorkflowStateName.END);
        myWorkflow.startNode(node1);
        myWorkflow.run();
        // when
        BufferedImage image = myWorkflow.generateComputedWorkflowBufferedImage();
        // then
        assertThat(image).isNotNull();
        assertThat(image.getType()).isEqualTo(BufferedImage.TYPE_INT_ARGB);
        assertThat(image.getWidth()).isGreaterThan(0);
        assertThat(image.getHeight()).isGreaterThan(0);
    }

}
