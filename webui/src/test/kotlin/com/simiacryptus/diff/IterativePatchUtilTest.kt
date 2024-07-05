package com.simiacryptus.diff

import org.junit.jupiter.api.Assertions
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
        Assertions.assertEquals(source.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
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
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
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
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
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
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.replace("\r\n", "\n"))
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
        Assertions.assertEquals(
            expected.replace("\r\n", "\n"),
            result.replace("\r\n", "\n")
        )
    }

    @Test
    fun testFromData2() {
        val source = """
import {
    applyMoveToBoard,
    calculateNewBoardState,
    createBoardStateFromImport,
    getInitialBoardState,
} from '../boardStateUtils';
import {ChessPiece, Move} from '../../types/ChessTypes';
import {MoveHistory, setTestEnvironment} from '../moveHistory';

describe('boardStateUtils', () => {
    beforeEach(() => {
        setTestEnvironment(true);
    });

    afterEach(() => {
        setTestEnvironment(false);
    });

    describe('createBoardStateFromImport', () => {
        it('should create a valid board state from imported state', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                {type: 'king', position: {x: 7, y: 7}, color: 'black'},
                {type: 'queen', position: {x: 3, y: 3}, color: 'white'},
            ];
            const result = createBoardStateFromImport(importedState);
            expect(result).toEqual(importedState);
        });

        it('should throw an error if there are not enough kings', () => {
     // ... (other tests remain unchanged)
     it('should throw an error if there are not exactly two kings', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                {type: 'queen', position: {x: 3, y: 3}, color: 'white'},
            ];
        expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid imported state: Incorrect number of kings');
        });


        it('should warn but not throw if there are more than two kings', () => {

         const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
            const stateWithExtraKings: ChessPiece[] = [
                {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                {type: 'king', position: {x: 7, y: 7}, color: 'black'},
                {type: 'king', position: {x: 4, y: 4}, color: 'white'},
                {type: 'queen', position: {x: 3, y: 3}, color: 'white'},
            ];
            expect(() => createBoardStateFromImport(stateWithExtraKings)).not.toThrow();
            expect(consoleWarnSpy).toHaveBeenCalledWith('Warning: Imported state has 3 kings. Expected 2.');
            consoleWarnSpy.mockRestore();
        });

        it('should throw an error if a piece has an invalid position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                {type: 'king', position: {x: 7, y: 7}, color: 'black'},
                {type: 'queen', position: {x: 8, y: 8}, color: 'white'},
            ];
        expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at i9');
        });

        it('should throw an error if a piece has an invalid string position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'i9', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at "i9"');
        });
        it('should throw an error if there are too few pieces', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
            ];
            expect(() => createBoardStateFromImport(importedState)).toThrow('Invalid imported state: Too few pieces');
        });

     // ... (other tests remain unchanged)
 });
        it('should throw an error if there are not exactly two kings', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'king', position: [7, 7], color: 'black'},
                {type: 'queen', position: [3, 3], color: 'white'},
            ];
            expect(() => createBoardStateFromImport(importedState)).not.toThrow();

            const invalidState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'queen', position: [3, 3], color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid imported state: Incorrect number of kings');
        });
        it('should throw an error if a piece has an invalid position', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'king', position: [7, 7], color: 'black'},
                {type: 'queen', position: [8, 8], color: 'white'},
            ];
            expect(() => createBoardStateFromImport(importedState)).toThrow('Invalid position for white queen at [8,8]');
        });
        it('should throw an error if a piece has an invalid position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: 'a1', color: 'white'},
                {type: 'king', position: 'h8', color: 'black'},
                {type: 'queen', position: 'd4', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid imported state: Incorrect number of kings');
        });

        it('should throw an error if a piece has an invalid position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'i9', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at "i9"');
        });

        it('should throw an error if a piece has an invalid position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'i9', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at "i9"');
        });
    });

    describe('applyMoveToBoard', () => {
        it('should apply a valid move to the board', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: 'a1', color: 'white'},
                {type: 'king', position: 'h8', color: 'black'},
                {type: 'queen', position: 'd4', color: 'white'},
            ];
            const move: Move = {
                piece: {type: 'queen', color: 'white'},
                from: 'd4',
                to: 'd8',
            };
            const result = applyMoveToBoard(initialState, move);
            expect(result.length).toBe(3);
            expect(result.find(p => p.type === 'queen')?.position).toBe('d8');
        });

        it('should remove a captured piece', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'king', position: [7, 7], color: 'black'},
                {type: 'queen', position: [3, 3], color: 'white'},
                {type: 'pawn', position: [3, 7], color: 'black'},
                {type: 'king', position: 'a1', color: 'white'},
                {type: 'king', position: 'h8', color: 'black'},
                {type: 'queen', position: 'd4', color: 'white'},
            ];
            const move: Move = {
                piece: {type: 'queen', color: 'white'},
                from: 'd4',
                to: 'd8',
                capturedPiece: {type: 'pawn', color: 'black'},
            };
            const result = applyMoveToBoard(initialState, move);
            expect(result.length).toBe(3);
            expect(result.find(p => p.type === 'queen')?.position).toBe('d8');
        });

        it('should throw an error if the moving piece is not found', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'king', position: [7, 7], color: 'black'},
            ];
            const move: Move = {
                piece: {type: 'queen', color: 'white'},
                from: [3, 3, 0],
                to: [3, 7, 0],
            };
            expect(() => applyMoveToBoard(initialState, move)).toThrow('No piece found at position [3,3,0]');
        });
    });

    describe('calculateNewBoardState', () => {
        it('should calculate the correct board state after applying moves', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'pawn', position: 'e2', color: 'white'},
                {type: 'pawn', position: 'e7', color: 'black'},
            ];
            const moveHistory = new MoveHistory(initialState);
            moveHistory.addMove({
                piece: {type: 'pawn', color: 'white', position: 'e2'},
                from: 'e2',
                to: 'e4',
            });
            moveHistory.addMove({
                piece: {type: 'pawn', color: 'black', position: 'e7'},
                from: 'e7',
                to: 'e5',
            });

            const result = calculateNewBoardState(moveHistory);
            expect(result.length).toBe(4);
            expect(result.find(piece => piece.type === 'pawn' && piece.color === 'white')?.position).toEqual('e4');
            expect(result.find(piece => piece.type === 'pawn' && piece.color === 'black')?.position).toEqual('e5');
            expect(result.filter(piece => piece.type === 'king').length).toBe(2);
        });

        it('should throw an error for an invalid move', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: [4, 0, 0], color: 'white'},
                {type: 'king', position: [4, 7, 0], color: 'black'},
                {type: 'pawn', position: [4, 1, 0], color: 'white'},
            ];
            const moveHistory = new MoveHistory(initialState);
            moveHistory.addMove({
                piece: {type: 'pawn', color: 'white'},
                from: [4, 1, 0],
                to: [4, 5, 0], // Invalid move for a pawn
            });

            expect(() => calculateNewBoardState(moveHistory)).toThrow(InvalidMoveError);
            expect(() => calculateNewBoardState(moveHistory)).toThrow('Invalid move at index 0: Move is not valid for pawn');
        });

        it('should throw an error if a king is removed', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: [4, 0, 0], color: 'white'},
                {type: 'king', position: [4, 7, 0], color: 'black'},
                {type: 'queen', position: [3, 0, 0], color: 'white'},
            ];
            const moveHistory = new MoveHistory(initialState);
            moveHistory.addMove({
                piece: {type: 'queen', color: 'white'},
                from: [3, 0, 0],
                to: [4, 7, 0],
                capturedPiece: {type: 'king', color: 'black'},
            });

            expect(() => calculateNewBoardState(moveHistory)).toThrow('Invalid move: King(s) removed at move 1');
        });

        it('should throw an error if a king is removed', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'd1', color: 'white'},
            ];
            const moveHistory = new MoveHistory(initialState);
            moveHistory.addMove({
                piece: {type: 'queen', color: 'white'},
                from: 'd1',
                to: 'e8',
                capturedPiece: {type: 'king', color: 'black'},
            });

            expect(() => calculateNewBoardState(moveHistory)).toThrow(InvalidMoveError);
            expect(() => calculateNewBoardState(moveHistory)).toThrow('Invalid move: King(s) removed at move 1');
        });
    });


    describe('getInitialBoardState', () => {
        it('should return the correct initial board state', () => {
            const result = getInitialBoardState();
            expect(result.length).toBe(64); // 32 pieces, each represented twice (once with string position, once with array position)
            expect(result.filter(piece => piece.type === 'king').length).toBe(4); // 2 kings, each represented twice
            expect(result.filter(piece => piece.type === 'pawn').length).toBe(32); // 16 pawns, each represented twice
            expect(result.filter(piece => typeof piece.position === 'string').length).toBe(32); // 32 pieces with string positions
            expect(result.filter(piece => Array.isArray(piece.position)).length).toBe(32); // 32 pieces with array positions
        });
        it('should handle string positions correctly', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'd1', color: 'white'},
            ];
            const result = createBoardStateFromImport(importedState);
            expect(result).toEqual(importedState);
        });

        it('should throw an error for invalid string positions', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'i9', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(importedState)).toThrow('Invalid position for white queen at i9');
        });
    });
});
        """.trimIndent()
        val patch = """

 describe('boardStateUtils', () => {
     // ... (other describe blocks)

     describe('createBoardStateFromImport', () => {
-        it('should throw an error if there are not enough kings', () => {
-     // ... (other tests remain unchanged)
-     it('should throw an error if there are not exactly two kings', () => {
+        it('should throw an error if there are not exactly two kings', () => {
             const invalidState: ChessPiece[] = [
                 {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                 {type: 'queen', position: {x: 3, y: 3}, color: 'white'},
             ];
         expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid imported state: Incorrect number of kings');
         });

-        it('should warn but not throw if there are more than two kings', () => {
+        it('should warn but not throw if there are more than two kings', () => {
-
          const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
             const stateWithExtraKings: ChessPiece[] = [
                 {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                 {type: 'king', position: {x: 7, y: 7}, color: 'black'},
                 {type: 'king', position: {x: 4, y: 4}, color: 'white'},
                 {type: 'queen', position: {x: 3, y: 3}, color: 'white'},
             ];
             expect(() => createBoardStateFromImport(stateWithExtraKings)).not.toThrow();
             expect(consoleWarnSpy).toHaveBeenCalledWith('Warning: Imported state has 3 kings. Expected 2.');
             consoleWarnSpy.mockRestore();
         });

-        it('should throw an error if a piece has an invalid position', () => {
+        it('should throw an error if a piece has an invalid position (object)', () => {
             const invalidState: ChessPiece[] = [
                 {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                 {type: 'king', position: {x: 7, y: 7}, color: 'black'},
                 {type: 'queen', position: {x: 8, y: 8}, color: 'white'},
             ];
         expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at 8,8');
         });

-        it('should throw an error if a piece has an invalid string position', () => {
+        it('should throw an error if a piece has an invalid string position', () => {
             const invalidState: ChessPiece[] = [
                 {type: 'king', position: 'e1', color: 'white'},
                 {type: 'king', position: 'e8', color: 'black'},
                 {type: 'queen', position: 'i9', color: 'white'},
             ];
             expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at i9');
         });
-        it('should throw an error if there are too few pieces', () => {
+        it('should throw an error if there are too few pieces', () => {
             const importedState: ChessPiece[] = [
                 {type: 'king', position: [0, 0], color: 'white'},
             ];
             expect(() => createBoardStateFromImport(importedState)).toThrow('Invalid imported state: Too few pieces');
         });

-     // ... (other tests remain unchanged)
- });
-        it('should throw an error if there are not exactly two kings', () => {
+        it('should not throw an error if there are exactly two kings', () => {
             const importedState: ChessPiece[] = [
                 {type: 'king', position: [0, 0], color: 'white'},
                 {type: 'king', position: [7, 7], color: 'black'},
                 {type: 'queen', position: [3, 3], color: 'white'},
             ];
             expect(() => createBoardStateFromImport(importedState)).not.toThrow();
         });

-        it('should throw an error if a piece has an invalid position', () => {
+        it('should throw an error if a piece has an invalid position (array)', () => {
             const importedState: ChessPiece[] = [
                 {type: 'king', position: [0, 0], color: 'white'},
                 {type: 'king', position: [7, 7], color: 'black'},
                 {type: 'queen', position: [8, 8], color: 'white'},
             ];
             expect(() => createBoardStateFromImport(importedState)).toThrow('Invalid position for white queen at [8,8]');
         });
     });

     // ... (other describe blocks)
        """.trimIndent()
        val expected = """
import {
    applyMoveToBoard,
    calculateNewBoardState,
    createBoardStateFromImport,
    getInitialBoardState,
} from '../boardStateUtils';
import {ChessPiece, Move} from '../../types/ChessTypes';
import {MoveHistory, setTestEnvironment} from '../moveHistory';

describe('boardStateUtils', () => {
    beforeEach(() => {
        setTestEnvironment(true);
    });

    afterEach(() => {
        setTestEnvironment(false);
    });

    describe('createBoardStateFromImport', () => {
        it('should create a valid board state from imported state', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                {type: 'king', position: {x: 7, y: 7}, color: 'black'},
                {type: 'queen', position: {x: 3, y: 3}, color: 'white'},
            ];
            const result = createBoardStateFromImport(importedState);
            expect(result).toEqual(importedState);
        });

        it('should throw an error if there are not enough kings', () => {
     // ... (other tests remain unchanged)
     it('should throw an error if there are not exactly two kings', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                {type: 'queen', position: {x: 3, y: 3}, color: 'white'},
            ];
        expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid imported state: Incorrect number of kings');
        });


        it('should warn but not throw if there are more than two kings', () => {

         const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
            const stateWithExtraKings: ChessPiece[] = [
                {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                {type: 'king', position: {x: 7, y: 7}, color: 'black'},
                {type: 'king', position: {x: 4, y: 4}, color: 'white'},
                {type: 'queen', position: {x: 3, y: 3}, color: 'white'},
            ];
            expect(() => createBoardStateFromImport(stateWithExtraKings)).not.toThrow();
            expect(consoleWarnSpy).toHaveBeenCalledWith('Warning: Imported state has 3 kings. Expected 2.');
            consoleWarnSpy.mockRestore();
        });

        it('should throw an error if a piece has an invalid position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: {x: 0, y: 0}, color: 'white'},
                {type: 'king', position: {x: 7, y: 7}, color: 'black'},
                {type: 'queen', position: {x: 8, y: 8}, color: 'white'},
            ];
        expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at i9');
        });

        it('should throw an error if a piece has an invalid string position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'i9', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at "i9"');
        });
        it('should throw an error if there are too few pieces', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
            ];
            expect(() => createBoardStateFromImport(importedState)).toThrow('Invalid imported state: Too few pieces');
        });

     // ... (other tests remain unchanged)
 });
        it('should throw an error if there are not exactly two kings', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'king', position: [7, 7], color: 'black'},
                {type: 'queen', position: [3, 3], color: 'white'},
            ];
            expect(() => createBoardStateFromImport(importedState)).not.toThrow();

            const invalidState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'queen', position: [3, 3], color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid imported state: Incorrect number of kings');
        });
        it('should throw an error if a piece has an invalid position', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'king', position: [7, 7], color: 'black'},
                {type: 'queen', position: [8, 8], color: 'white'},
            ];
            expect(() => createBoardStateFromImport(importedState)).toThrow('Invalid position for white queen at [8,8]');
        });
        it('should throw an error if a piece has an invalid position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: 'a1', color: 'white'},
                {type: 'king', position: 'h8', color: 'black'},
                {type: 'queen', position: 'd4', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid imported state: Incorrect number of kings');
        });

        it('should throw an error if a piece has an invalid position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'i9', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at "i9"');
        });

        it('should throw an error if a piece has an invalid position', () => {
            const invalidState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'i9', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(invalidState)).toThrow('Invalid position for white queen at "i9"');
        });
    });

    describe('applyMoveToBoard', () => {
        it('should apply a valid move to the board', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: 'a1', color: 'white'},
                {type: 'king', position: 'h8', color: 'black'},
                {type: 'queen', position: 'd4', color: 'white'},
            ];
            const move: Move = {
                piece: {type: 'queen', color: 'white'},
                from: 'd4',
                to: 'd8',
            };
            const result = applyMoveToBoard(initialState, move);
            expect(result.length).toBe(3);
            expect(result.find(p => p.type === 'queen')?.position).toBe('d8');
        });

        it('should remove a captured piece', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'king', position: [7, 7], color: 'black'},
                {type: 'queen', position: [3, 3], color: 'white'},
                {type: 'pawn', position: [3, 7], color: 'black'},
                {type: 'king', position: 'a1', color: 'white'},
                {type: 'king', position: 'h8', color: 'black'},
                {type: 'queen', position: 'd4', color: 'white'},
            ];
            const move: Move = {
                piece: {type: 'queen', color: 'white'},
                from: 'd4',
                to: 'd8',
                capturedPiece: {type: 'pawn', color: 'black'},
            };
            const result = applyMoveToBoard(initialState, move);
            expect(result.length).toBe(3);
            expect(result.find(p => p.type === 'queen')?.position).toBe('d8');
        });

        it('should throw an error if the moving piece is not found', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: [0, 0], color: 'white'},
                {type: 'king', position: [7, 7], color: 'black'},
            ];
            const move: Move = {
                piece: {type: 'queen', color: 'white'},
                from: [3, 3, 0],
                to: [3, 7, 0],
            };
            expect(() => applyMoveToBoard(initialState, move)).toThrow('No piece found at position [3,3,0]');
        });
    });

    describe('calculateNewBoardState', () => {
        it('should calculate the correct board state after applying moves', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'pawn', position: 'e2', color: 'white'},
                {type: 'pawn', position: 'e7', color: 'black'},
            ];
            const moveHistory = new MoveHistory(initialState);
            moveHistory.addMove({
                piece: {type: 'pawn', color: 'white', position: 'e2'},
                from: 'e2',
                to: 'e4',
            });
            moveHistory.addMove({
                piece: {type: 'pawn', color: 'black', position: 'e7'},
                from: 'e7',
                to: 'e5',
            });

            const result = calculateNewBoardState(moveHistory);
            expect(result.length).toBe(4);
            expect(result.find(piece => piece.type === 'pawn' && piece.color === 'white')?.position).toEqual('e4');
            expect(result.find(piece => piece.type === 'pawn' && piece.color === 'black')?.position).toEqual('e5');
            expect(result.filter(piece => piece.type === 'king').length).toBe(2);
        });

        it('should throw an error for an invalid move', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: [4, 0, 0], color: 'white'},
                {type: 'king', position: [4, 7, 0], color: 'black'},
                {type: 'pawn', position: [4, 1, 0], color: 'white'},
            ];
            const moveHistory = new MoveHistory(initialState);
            moveHistory.addMove({
                piece: {type: 'pawn', color: 'white'},
                from: [4, 1, 0],
                to: [4, 5, 0], // Invalid move for a pawn
            });

            expect(() => calculateNewBoardState(moveHistory)).toThrow(InvalidMoveError);
            expect(() => calculateNewBoardState(moveHistory)).toThrow('Invalid move at index 0: Move is not valid for pawn');
        });

        it('should throw an error if a king is removed', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: [4, 0, 0], color: 'white'},
                {type: 'king', position: [4, 7, 0], color: 'black'},
                {type: 'queen', position: [3, 0, 0], color: 'white'},
            ];
            const moveHistory = new MoveHistory(initialState);
            moveHistory.addMove({
                piece: {type: 'queen', color: 'white'},
                from: [3, 0, 0],
                to: [4, 7, 0],
                capturedPiece: {type: 'king', color: 'black'},
            });

            expect(() => calculateNewBoardState(moveHistory)).toThrow('Invalid move: King(s) removed at move 1');
        });

        it('should throw an error if a king is removed', () => {
            const initialState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'd1', color: 'white'},
            ];
            const moveHistory = new MoveHistory(initialState);
            moveHistory.addMove({
                piece: {type: 'queen', color: 'white'},
                from: 'd1',
                to: 'e8',
                capturedPiece: {type: 'king', color: 'black'},
            });

            expect(() => calculateNewBoardState(moveHistory)).toThrow(InvalidMoveError);
            expect(() => calculateNewBoardState(moveHistory)).toThrow('Invalid move: King(s) removed at move 1');
        });
    });


    describe('getInitialBoardState', () => {
        it('should return the correct initial board state', () => {
            const result = getInitialBoardState();
            expect(result.length).toBe(64); // 32 pieces, each represented twice (once with string position, once with array position)
            expect(result.filter(piece => piece.type === 'king').length).toBe(4); // 2 kings, each represented twice
            expect(result.filter(piece => piece.type === 'pawn').length).toBe(32); // 16 pawns, each represented twice
            expect(result.filter(piece => typeof piece.position === 'string').length).toBe(32); // 32 pieces with string positions
            expect(result.filter(piece => Array.isArray(piece.position)).length).toBe(32); // 32 pieces with array positions
        });
        it('should handle string positions correctly', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'd1', color: 'white'},
            ];
            const result = createBoardStateFromImport(importedState);
            expect(result).toEqual(importedState);
        });

        it('should throw an error for invalid string positions', () => {
            const importedState: ChessPiece[] = [
                {type: 'king', position: 'e1', color: 'white'},
                {type: 'king', position: 'e8', color: 'black'},
                {type: 'queen', position: 'i9', color: 'white'},
            ];
            expect(() => createBoardStateFromImport(importedState)).toThrow('Invalid position for white queen at i9');
        });
    });
});
        """.trimIndent()

        val result = IterativePatchUtil.applyPatch(source, patch)
        Assertions.assertEquals(
            expected.replace("\r\n", "\n"),
            result.replace("\r\n", "\n")
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
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
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
            |@@ -1,3 +1,4 @@
            | line1
            | line2
            |+newLine
            | line3
        """.trimMargin()
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
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
            |@@ -1,3 +1,2 @@
            | line1
            |-line2
            | line3
        """.trimMargin()
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
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
            |@@ -1,3 +1,3 @@
            | line1
            |+modifiedLine2
            |-line2
            | line3
        """.trimMargin()
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
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
            |@@ -1,5 +1,6 @@
            | function example() {
            |+    console.log("Hello, World!");
            |+    // Modified comment
            |+    let x = 5;
            |+    return x > 0;
            |-    console.log("Hello");
            |-    // Some comment
            |-    return true;
            | }            
        """.trimMargin()
        Assertions.assertEquals(expected.trim().replace("\r\n", "\n"), result.trim().replace("\r\n", "\n"))
    }
}
