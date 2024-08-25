package com.simiacryptus.diff

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IterativePatchUtilTest {

    @Test
    fun testPatchExactMatch() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
            line2
            line3
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        assertEquals(source.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    // ... (other existing tests)
    @Test
    fun testPatchAddLine() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
            line2
            +newLine
            line3
        """.trimIndent()
        val expected = """
            line1
            line2
            newLine
            line3
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    @Test
    fun testPatchModifyLine() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
            -line2
            +modifiedLine2
            line3
        """.trimIndent()
        val expected = """
            line1
            modifiedLine2
            line3
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    @Test
    fun testPatchModifyLineWithComments() {
        val source = """
            line1
            line3
            line2
        """.trimIndent()
        val patch = """
            line1
            line3
            // This comment should be ignored
            -line2
            +modifiedLine2
            # LLMs sometimes get chatty and add stuff to patches__
        """.trimIndent()
        val expected = """
            line1
            line3
            modifiedLine2
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    @Test
    fun testPatchRemoveLine() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
          - line2
            line3
        """.trimIndent()
        val expected = """
            line1
            line3
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    @Test
    fun testPatchAdd2Line2() {
        val source = """
            line1
            
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
          + lineA
          
          + lineB
          
            line2
            line3
        """.trimIndent()
        val expected = """
           |line1
           | lineA
           | lineB
           |
           |line2
           |line3
        """.trimMargin()
        val result = IterativePatchUtil.applyPatch(source, patch)
        assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    @Test
    fun testPatchAdd2Line3() {
        val source = """
            line1
            
            line2
            line3
        """.trimIndent()
        val patch = """
            line1
            // extraneous comment
          + lineA
          + lineB
            // llms sometimes get chatty and add stuff to patches
            line2
            line3
        """.trimIndent()
        val expected = """
           |line1
           | lineA
           | lineB
           |
           |line2
           |line3
        """.trimMargin()
        val result = IterativePatchUtil.applyPatch(source, patch)
        assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

    @Test
    fun testFromData1() {
        val source = """
        function updateTabs() {
            document.querySelectorAll('.tab-button').forEach(button => {
                button.addEventListener('click', (event) => { // Ensure the event is passed as a parameter
                    event.stopPropagation();
                    const forTab = button.getAttribute('data-for-tab');
                    let tabsParent = button.closest('.tabs-container');
                    tabsParent.querySelectorAll('.tab-content').forEach(content => {
                        const contentParent = content.closest('.tabs-container');
                        if (contentParent === tabsParent) {
                            if (content.getAttribute('data-tab') === forTab) {
                                content.classList.add('active');
                            } else if (content.classList.contains('active')) {
                                content.classList.remove('active')
                            }
                        }
                    });
                })
            });
        }
        """.trimIndent()
        val patch = """
        tabsParent.querySelectorAll('.tab-content').forEach(content => {
            const contentParent = content.closest('.tabs-container');
            if (contentParent === tabsParent) {
                if (content.getAttribute('data-tab') === forTab) {
                    content.classList.add('active');
        +           button.classList.add('active'); // Mark the button as active
                } else if (content.classList.contains('active')) {
                    content.classList.remove('active')
        +           button.classList.remove('active'); // Ensure the button is not marked as active
                }
            }
        });
        """.trimIndent()
        val expected = """
        function updateTabs() {
            document.querySelectorAll('.tab-button').forEach(button => {
                button.addEventListener('click', (event) => { // Ensure the event is passed as a parameter
                    event.stopPropagation();
                    const forTab = button.getAttribute('data-for-tab');
                    let tabsParent = button.closest('.tabs-container');
                    tabsParent.querySelectorAll('.tab-content').forEach(content => {
                        const contentParent = content.closest('.tabs-container');
                        if (contentParent === tabsParent) {
                            if (content.getAttribute('data-tab') === forTab) {
                                content.classList.add('active');
                                button.classList.add('active'); // Mark the button as active
                            } else if (content.classList.contains('active')) {
                                content.classList.remove('active')
                                button.classList.remove('active'); // Ensure the button is not marked as active
                            }
                        }
                    });
                })
            });
        }
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        assertEquals(
            expected.replace("\\s*\r?\n\\s*".toRegex(), "\n"),
            result.replace("\\s*\r?\n\\s*".toRegex(), "\n")
        )
    }

    @Test
    fun testFromData2() {
        val source = """
            export class StandardChessModel implements GameModel {
                geometry: BoardGeometry;
                state: GameState;
                private moveHistory: MoveHistory;
    
                constructor(initialBoard?: Piece[]) {
                    this.geometry = new StandardBoardGeometry();
                    this.state = initialBoard ? this.initializeWithBoard(initialBoard) : this.initialize();
                    this.moveHistory = new MoveHistory(this.state.board);
                }
    
                redoMove(): GameState {
                    return this.getState();
                }
    
                isGameOver(): boolean {
                    return false;
                }
    
                getWinner(): 'white' | 'black' | 'draw' | null {
                    return null;
                }
    
                importState(stateString: string): GameState {
                    // Implement import state logic
                    const parsedState = JSON.parse(stateString);
                    // Validate and convert the parsed state to GameState
                    // For now, we'll just return the current state
                    return this.getState();
                }
    
            }
    
            // Similar changes for black pawns
        """.trimIndent()
        val patch = """
        | export class StandardChessModel implements GameModel {
        |     // ... other methods ...
        |
        |-    getWinner(): 'white' | 'black' | 'draw' | null {
        |+    getWinner(): ChessColor | 'draw' | null {
        |         return null;
        |     }
        |
        |     // ... other methods ...
        | }
        """.trimMargin()
        val expected = """
            export class StandardChessModel implements GameModel {
                geometry: BoardGeometry;
                state: GameState;
                private moveHistory: MoveHistory;
    
                constructor(initialBoard?: Piece[]) {
                    this.geometry = new StandardBoardGeometry();
                    this.state = initialBoard ? this.initializeWithBoard(initialBoard) : this.initialize();
                    this.moveHistory = new MoveHistory(this.state.board);
                }
    
                redoMove(): GameState {
                    return this.getState();
                }
    
                isGameOver(): boolean {
                    return false;
                }
    
                getWinner(): ChessColor | 'draw' | null {
                    return null;
                }
    
                importState(stateString: string): GameState {
                    // Implement import state logic
                    const parsedState = JSON.parse(stateString);
                    // Validate and convert the parsed state to GameState
                    // For now, we'll just return the current state
                    return this.getState();
                }
    
            }
    
            // Similar changes for black pawns
        """.trimIndent()
        val result = IterativePatchUtil.applyPatch(source, patch)
        assertEquals(
            expected.replace("\\s*\r?\n\\s*".toRegex(), "\n"),
            result.replace("\\s*\r?\n\\s*".toRegex(), "\n")
        )
    }


    @Test
    fun testGeneratePatchNoChanges() {
        val oldCode = """
            |line1
            |line2
            |line3
        """.trimMargin()
        val newCode = oldCode
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
        """.trimMargin()
        assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
    }

    @Test
    fun testGeneratePatchAddLine() {
        val oldCode = """
            |line1
            |line2
            |line3
        """.trimMargin()
        val newCode = """
            |line1
            |line2
            |newLine
            |line3
        """.trimMargin()
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
            |  line1
            |  line2
            |+ newLine
            |  line3
        """.trimMargin()
        assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
    }

    @Test
    fun testGeneratePatchRemoveLine() {
        val oldCode = """
            line1
            line2
            line3
        """.trimIndent()
        val newCode = """
            line1
            line3
        """.trimIndent()
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
            |  line1
            |- line2
            |  line3
        """.trimMargin()
        assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
    }

    @Test
    fun testGeneratePatchModifyLine() {
        val oldCode = """
            line1
            line2
            line3
        """.trimIndent()
        val newCode = """
            line1
            modifiedLine2
            line3
        """.trimIndent()
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
            |  line1
            |- line2
            |+ modifiedLine2
            |  line3
        """.trimMargin()
        assertEquals(
            expected.trim().replace("\r\n", "\n"),
            result.trim().replace("\r\n", "\n")
        )
    }

    @Test
    fun testGeneratePatchComplexChanges() {
        val oldCode = """
            function example() {
                console.log("Hello");
                // Some comment
                return true;
            }
        """.trimIndent()
        val newCode = """
            function example() {
                console.log("Hello, World!");
                // Modified comment
                let x = 5;
                return x > 0;
            }
        """.trimIndent()
        val result = IterativePatchUtil.generatePatch(oldCode, newCode)
        val expected = """
            |  function example() {
            |-     console.log("Hello");
            |-     // Some comment
            |-     return true;
            |+     console.log("Hello, World!");
            |+     // Modified comment
            |+     let x = 5;
            |+     return x > 0;
            |  }
        """.trimMargin()
        assertEquals(
            expected.trim().replace("\r\n", "\n"),
            result.trim().replace("\r\n", "\n")
        )
    }

    @Test
    fun testGeneratePatchMoveLineUpwardsMultiplePositions() {
        val oldCode = """
            line1
            line2
            line3
            line4
            line5
            line6
        """.trimIndent()

        val newCode = """
            line1
            line5
            line2
            line3
            line4
            line6
        """.trimIndent()

        val expectedPatch = """
              line1
            - line2
            - line3
            - line4
              line5
            + line2
            + line3
            + line4
              line6
        """.trimIndent()

        val actualPatch = IterativePatchUtil.generatePatch(oldCode, newCode)
        assertEquals(expectedPatch, actualPatch)
    }

    @Test
    fun testGeneratePatchMoveLineDownwardsMultiplePositions() {
        val oldCode = """
            line1
            line2
            line3
            line4
            line5
            line6
        """.trimIndent()

        val newCode = """
            line1
            line3
            line4
            line5
            line6
            line2
        """.trimIndent()

        val expectedPatch = """
              line1
            - line2
              line3
              line4
              line5
              line6
            + line2
        """.trimIndent()

        val actualPatch = IterativePatchUtil.generatePatch(oldCode, newCode)
        assertEquals(expectedPatch, actualPatch)
    }

    @Test
    fun testGeneratePatchSwapLines() {
        val oldCode = """
            line1
            line2
            line3
            line4
            line5
            line6
        """.trimIndent()

        val newCode = """
            line1
            line4
            line3
            line2
            line5
            line6
        """.trimIndent()

        val expectedPatch = """
              line1
            - line2
            - line3
              line4
            + line3
            + line2
              line5
              line6
        """.trimIndent()

        val actualPatch = IterativePatchUtil.generatePatch(oldCode, newCode)
        assertEquals(expectedPatch, actualPatch)
    }

    @Test
    fun testGeneratePatchMoveAdjacentLines() {
        val oldCode = """
            line1
            line2
            line3
            line4
            line5
            line6
        """.trimIndent()

        val newCode = """
            line1
            line4
            line5
            line2
            line3
            line6
        """.trimIndent()

        val expectedPatch = """
              line1
            - line2
            - line3
              line4
              line5
            + line2
            + line3
              line6
        """.trimIndent()

        val actualPatch = IterativePatchUtil.generatePatch(oldCode, newCode)
        assertEquals(expectedPatch, actualPatch)
    }

    @Test
    fun testGeneratePatchMoveLineUpwards() {
        val oldCode = """
            line1
            line2
            line3
            line4
            line5
            line6
        """.trimIndent()
        val newCode = """
            line1
            line2
            line5
            line3
            line4
            line6
        """.trimIndent()
        val expectedPatch = """
              line1
              line2
            - line3
            - line4
              line5
            + line3
            + line4
              line6
        """.trimIndent()
        val actualPatch = IterativePatchUtil.generatePatch(oldCode, newCode)
        assertEquals(expectedPatch, actualPatch)
    }

    @Test
    fun testGeneratePatchMoveLineDownwards() {
        val oldCode = """
            line1
            line2
            line3
            line4
            line5
            line6
        """.trimIndent()
        val newCode = """
            line1
            line3
            line4
            line5
            line2
            line6
        """.trimIndent()
        val expectedPatch = """
              line1
            - line2
              line3
              line4
              line5
            + line2
              line6
        """.trimIndent()
        val actualPatch = IterativePatchUtil.generatePatch(oldCode, newCode)
        assertEquals(expectedPatch, actualPatch)
    }
}
