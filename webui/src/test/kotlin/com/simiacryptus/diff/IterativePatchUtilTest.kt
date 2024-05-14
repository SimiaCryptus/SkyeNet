package com.simiacryptus.diff

import org.intellij.lang.annotations.Language
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
        val result = IterativePatchUtil.patch(source, patch)
        Assertions.assertEquals(source.replace("\r\n", "\n"), result.replace("\r\n", "\n"))
    }

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
        val result = IterativePatchUtil.patch(source, patch)
        Assertions.assertEquals(expected.replace("\r\n", "\n"), result.replace("\r\n", "\n"))
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
        val result = IterativePatchUtil.patch(source, patch)
        Assertions.assertEquals(expected.replace("\r\n", "\n"), result.replace("\r\n", "\n"))
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
        val result = IterativePatchUtil.patch(source, patch)
        Assertions.assertEquals(expected.replace("\r\n", "\n"), result.replace("\r\n", "\n"))
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
        val result = IterativePatchUtil.patch(source, patch)
        Assertions.assertEquals(
            expected.replace("\r\n", "\n").replace("\\s{1,}".toRegex(), " "),
            result.replace("\r\n", "\n").replace("\\s{1,}".toRegex(), " ")
        )
    }
    @Test
    fun testFromData2() {
        val source = """
        // Importing the maze data from mazeData.js
        import mazeData from './mazeData.js';
        
        // Helper function to find an open position in the maze
        function findOpenPosition(grid, startX, startY) {
            for (let y = startY; y < grid.length; y++) {
                for (let x = startX; x < grid[y].length; x++) {
                    if (grid[y][x] === 0) {
                        return { x: x, y: y };
                    }
                }
            }
            return { x: startX, y: startY }; // Fallback to the original position if no open position is found
        }
        // Variables to store maze size
        let mazeWidth = 10;
        let mazeHeight = 10;
        // Select the first level as default
        let { grid: maze, start, end } = mazeData.levels[0];
        let startPosition = { row: start.y, col: start.x };
        let endPosition = { row: end.y, col: end.x };
        // Variables to track the player's position and game status
        let playerPosition = {...startPosition};
        let gameRunning = false;
        let timerInterval;
        let timeElapsed = 0;
        
        // Function to initialize the game
        function initializeGame() {
            document.getElementById('resetButton').addEventListener('click', restartGame);
            document.getElementById('startButton').addEventListener('click', startGame);
            document.getElementById('setSizeButton').addEventListener('click', setMazeSize);
            document.addEventListener('keydown', handleKeyPress);
            document.addEventListener('touchstart', handleTouchStart, { passive: false });
            document.addEventListener('touchmove', handleTouchMove, { passive: false });
            drawMaze();
            placePlayer();
        }
        
        function setMazeSize() {
            mazeWidth = parseInt(document.getElementById('mazeWidth').value);
            mazeHeight = parseInt(document.getElementById('mazeHeight').value);
            const newGrid = mazeData.generateMaze(mazeHeight, mazeWidth);
            maze = newGrid; // Directly update the local 'maze' variable
            // Ensure start and end are not placed inside walls
            start = findOpenPosition(newGrid, 1, 1);
            end = findOpenPosition(newGrid, mazeWidth - 2, mazeHeight - 2);
            startPosition = { row: mazeData.levels[0].start.y, col: mazeData.levels[0].start.x };
            endPosition = { row: mazeData.levels[0].end.y, col: mazeData.levels[0].end.x };
            drawMaze();
            placePlayer();
        }
        let touchStartX = 0;
        let touchStartY = 0;
        
        function handleTouchStart(event) {
            touchStartX = event.touches[0].clientX;
            touchStartY = event.touches[0].clientY;
            event.preventDefault();
        }
        
        function handleTouchMove(event) {
            if (!touchStartX || !touchStartY) {
                return;
            }
        
            let touchMoveX = event.touches[0].clientX;
            let touchMoveY = event.touches[0].clientY;
            let diffX = touchStartX - touchMoveX;
            let diffY = touchStartY - touchMoveY;
        
            if (Math.abs(diffX) > Math.abs(diffY)) {
                // Horizontal movement
                if (diffX > 0) {
                    // Left swipe
                    handleKeyPress({ key: 'ArrowLeft' });
                } else {
                    // Right swipe
                    handleKeyPress({ key: 'ArrowRight' });
                }
            } else {
                // Vertical movement
                if (diffY > 0) {
                    // Up swipe
                    handleKeyPress({ key: 'ArrowUp' });
                } else {
                    // Down swipe
                    handleKeyPress({ key: 'ArrowDown' });
                }
            }
        
            // Reset values
            touchStartX = 0;
            touchStartY = 0;
            event.preventDefault();
        }
        // Function to draw the maze
        function drawMaze() {
            const mazeContainer = document.getElementById('gameArea');
            mazeContainer.innerHTML = ''; // Clear previous maze
        
            maze.forEach((row, rowIndex) => {
                row.forEach((cell, colIndex) => {
                    const cellElement = document.createElement('div');
                    cellElement.classList.add('cell', cell === 1 ? 'wall' : 'path');
                    if (rowIndex === endPosition.row && colIndex === endPosition.col) {
                        cellElement.classList.add('end');
                    }
                    cellElement.style.top = `${"$"}{rowIndex * 20}px`;
                    cellElement.style.left = `${"$"}{colIndex * 20}px`;
                    mazeContainer.appendChild(cellElement);
                });
            });
        }
        
        // Function to place the player in the starting position
        function placePlayer() {
            const playerElement = document.createElement('div');
            playerElement.id = 'player';
            playerElement.style.top = `${"$"}{playerPosition.row * 20}px`;
            playerElement.style.left = `${"$"}{playerPosition.col * 20}px`;
            document.getElementById('gameArea').appendChild(playerElement);
        }
        
        // Function to handle key press events
        function handleKeyPress(event) {
            if (!gameRunning) {
                startGame();
            }
        
            let newPosition = {...playerPosition};
            switch (event.key) {
                case 'ArrowUp':
                    newPosition.row--;
                    break;
                case 'ArrowDown':
                    newPosition.row++;
                    break;
                case 'ArrowLeft':
                    newPosition.col--;
                    break;
                case 'ArrowRight':
                    newPosition.col++;
                    break;
                default:
                    return; // Ignore other keys
            }
        
            if (isValidMove(newPosition)) {
                updatePlayerPosition(newPosition);
                checkWinCondition();
            }
        }
        
        // Function to check if the move is valid
        function isValidMove(position) {
            return maze[position.row] && maze[position.row][position.col] === 0;
        }
        
        // Function to update the player's position
        function updatePlayerPosition(position) {
            playerPosition = position;
            const playerElement = document.getElementById('player');
            playerElement.style.top = `${"$"}{position.row * 20}px`;
            playerElement.style.left = `${"$"}{position.col * 20}px`;
        }
        
        // Function to check win condition
        function checkWinCondition() {
            if (playerPosition.row === endPosition.row && playerPosition.col === endPosition.col) {
                clearInterval(timerInterval);
                gameRunning = false;
                alert('Congratulations! You have completed the maze.');
            }
        }
        
        // Function to start the game
        function startGame() {
            if (gameRunning) return; // Prevent restarting the game if it's already running
            gameRunning = true;
            timerInterval = setInterval(() => {
                timeElapsed++;
                document.getElementById('timer').textContent = formatTime(timeElapsed);
            }, 1000);
        }
        
        // Function to format time from seconds to MM:SS format
        function formatTime(seconds) {
            const minutes = Math.floor(seconds / 60);
            const remainingSeconds = seconds % 60;
            return `${"$"}{padZero(minutes)}:${"$"}{padZero(remainingSeconds)}`;
        }
        
        // Helper function to pad time values with zero
        function padZero(number) {
            return number.toString().padStart(2, '0');
        }
        
        // Function to restart the game
        function restartGame() {
            if (!gameRunning) startGame(); // Ensure game starts if not already running when reset
            clearInterval(timerInterval);
            timeElapsed = 0;
            document.getElementById('timer').textContent = '00:00';
            playerPosition = {...startPosition};
            placePlayer();
            gameRunning = false;
        }
        
        // Initialize the game when the window loads
        window.onload = initializeGame;
        """.trimIndent()
        @Language("TEXT") val patch = """
         // Function to draw the maze
         function drawMaze() {
             const mazeContainer = document.getElementById('gameArea');
             mazeContainer.innerHTML = ''; // Clear previous maze
        
        +    const cellWidth = mazeContainer.clientWidth / mazeWidth;
        +    const cellHeight = mazeContainer.clientHeight / mazeHeight;
        
             maze.forEach((row, rowIndex) => {
                 row.forEach((cell, colIndex) => {
                     const cellElement = document.createElement('div');
                     cellElement.classList.add('cell', cell === 1 ? 'wall' : 'path');
                     if (rowIndex === endPosition.row && colIndex === endPosition.col) {
                         cellElement.classList.add('end');
                     }
        -            cellElement.style.top = `${"$"}{rowIndex * 20}px`;
        -            cellElement.style.left = `${"$"}{colIndex * 20}px`;
        -            cellElement.style.width = '20px';
        -            cellElement.style.height = '20px';
        +            cellElement.style.top = `${"$"}{rowIndex * cellHeight}px`;
        +            cellElement.style.left = `${"$"}{colIndex * cellWidth}px`;
        +            cellElement.style.width = `${"$"}{cellWidth}px`;
        +            cellElement.style.height = `${"$"}{cellHeight}px`;
                     mazeContainer.appendChild(cellElement);
                 });
             });
         }
        
         // Function to place the player in the starting position
         function placePlayer() {
             const playerElement = document.createElement('div');
             playerElement.id = 'player';
        -    playerElement.style.top = `${"$"}{playerPosition.row * 20}px`;
        -    playerElement.style.left = `${"$"}{playerPosition.col * 20}px`;
        -    playerElement.style.width = '20px';
        -    playerElement.style.height = '20px';
        +    const cellWidth = document.getElementById('gameArea').clientWidth / mazeWidth;
        +    const cellHeight = document.getElementById('gameArea').clientHeight / mazeHeight;
        +    playerElement.style.top = `${"$"}{playerPosition.row * cellHeight}px`;
        +    playerElement.style.left = `${"$"}{playerPosition.col * cellWidth}px`;
        +    playerElement.style.width = `${"$"}{cellWidth}px`;
        +    playerElement.style.height = `${"$"}{cellHeight}px`;
             document.getElementById('gameArea').appendChild(playerElement);
         }
        
         // Function to update the player's position
         function updatePlayerPosition(position) {
             playerPosition = position;
             const playerElement = document.getElementById('player');
        -    playerElement.style.top = `${"$"}{position.row * 20}px`;
        -    playerElement.style.left = `${"$"}{position.col * 20}px`;
        +    const cellWidth = document.getElementById('gameArea').clientWidth / mazeWidth;
        +    const cellHeight = document.getElementById('gameArea').clientHeight / mazeHeight;
        +    playerElement.style.top = `${"$"}{position.row * cellHeight}px`;
        +    playerElement.style.left = `${"$"}{position.col * cellWidth}px`;
         }
        """.trimIndent()
        val expected = """
            // Importing the maze data from mazeData.js
            import mazeData from './mazeData.js';
            
            // Helper function to find an open position in the maze
            function findOpenPosition(grid, startX, startY) {
             for (let y = startY; y < grid.length; y++) {
             for (let x = startX; x < grid[y].length; x++) {
             if (grid[y][x] === 0) {
             return { x: x, y: y };
             }
             }
             }
             return { x: startX, y: startY }; // Fallback to the original position if no open position is found
            }
            // Variables to store maze size
            let mazeWidth = 10;
            let mazeHeight = 10;
            // Select the first level as default
            let { grid: maze, start, end } = mazeData.levels[0];
            let startPosition = { row: start.y, col: start.x };
            let endPosition = { row: end.y, col: end.x };
            // Variables to track the player's position and game status
            let playerPosition = {...startPosition};
            let gameRunning = false;
            let timerInterval;
            let timeElapsed = 0;
            
            // Function to initialize the game
            function initializeGame() {
             document.getElementById('resetButton').addEventListener('click', restartGame);
             document.getElementById('startButton').addEventListener('click', startGame);
             document.getElementById('setSizeButton').addEventListener('click', setMazeSize);
             document.addEventListener('keydown', handleKeyPress);
             document.addEventListener('touchstart', handleTouchStart, { passive: false });
             document.addEventListener('touchmove', handleTouchMove, { passive: false });
             drawMaze();
             placePlayer();
            }
            
            function setMazeSize() {
             mazeWidth = parseInt(document.getElementById('mazeWidth').value);
             mazeHeight = parseInt(document.getElementById('mazeHeight').value);
             const newGrid = mazeData.generateMaze(mazeHeight, mazeWidth);
             maze = newGrid; // Directly update the local 'maze' variable
             // Ensure start and end are not placed inside walls
             start = findOpenPosition(newGrid, 1, 1);
             end = findOpenPosition(newGrid, mazeWidth - 2, mazeHeight - 2);
             startPosition = { row: mazeData.levels[0].start.y, col: mazeData.levels[0].start.x };
             endPosition = { row: mazeData.levels[0].end.y, col: mazeData.levels[0].end.x };
             drawMaze();
             placePlayer();
            }
            let touchStartX = 0;
            let touchStartY = 0;
            
            function handleTouchStart(event) {
             touchStartX = event.touches[0].clientX;
             touchStartY = event.touches[0].clientY;
             event.preventDefault();
            }
            
            function handleTouchMove(event) {
             if (!touchStartX || !touchStartY) {
             return;
             }
            
             let touchMoveX = event.touches[0].clientX;
             let touchMoveY = event.touches[0].clientY;
             let diffX = touchStartX - touchMoveX;
             let diffY = touchStartY - touchMoveY;
            
             if (Math.abs(diffX) > Math.abs(diffY)) {
             // Horizontal movement
             if (diffX > 0) {
             // Left swipe
             handleKeyPress({ key: 'ArrowLeft' });
             } else {
             // Right swipe
             handleKeyPress({ key: 'ArrowRight' });
             }
             } else {
             // Vertical movement
             if (diffY > 0) {
             // Up swipe
             handleKeyPress({ key: 'ArrowUp' });
             } else {
             // Down swipe
             handleKeyPress({ key: 'ArrowDown' });
             }
             }
            
             // Reset values
             touchStartX = 0;
             touchStartY = 0;
             event.preventDefault();
            }
            // Function to draw the maze
            function drawMaze() {
             const mazeContainer = document.getElementById('gameArea');
             mazeContainer.innerHTML = ''; // Clear previous maze
            
             const cellWidth = mazeContainer.clientWidth / mazeWidth;
             const cellHeight = mazeContainer.clientHeight / mazeHeight;
            
             maze.forEach((row, rowIndex) => {
             row.forEach((cell, colIndex) => {
             const cellElement = document.createElement('div');
             cellElement.classList.add('cell', cell === 1 ? 'wall' : 'path');
             if (rowIndex === endPosition.row && colIndex === endPosition.col) {
             cellElement.classList.add('end');
             }
             cellElement.style.top = `${"$"}{rowIndex * cellHeight}px`;
             cellElement.style.left = `${"$"}{colIndex * cellWidth}px`;
             cellElement.style.width = `${"$"}{cellWidth}px`;
             cellElement.style.height = `${"$"}{cellHeight}px`;
             mazeContainer.appendChild(cellElement);
             });
             });
            }
            
            // Function to place the player in the starting position
            function placePlayer() {
             const playerElement = document.createElement('div');
             playerElement.id = 'player';
             const cellWidth = document.getElementById('gameArea').clientWidth / mazeWidth;
             const cellHeight = document.getElementById('gameArea').clientHeight / mazeHeight;
             playerElement.style.top = `${"$"}{playerPosition.row * cellHeight}px`;
             playerElement.style.left = `${"$"}{playerPosition.col * cellWidth}px`;
             playerElement.style.width = `${"$"}{cellWidth}px`;
             playerElement.style.height = `${"$"}{cellHeight}px`;
             document.getElementById('gameArea').appendChild(playerElement);
            }
            
            // Function to handle key press events
            function handleKeyPress(event) {
             if (!gameRunning) {
             startGame();
             }
            
             let newPosition = {...playerPosition};
             switch (event.key) {
             case 'ArrowUp':
             newPosition.row--;
             break;
             case 'ArrowDown':
             newPosition.row++;
             break;
             case 'ArrowLeft':
             newPosition.col--;
             break;
             case 'ArrowRight':
             newPosition.col++;
             break;
             default:
             return; // Ignore other keys
             }
            
             if (isValidMove(newPosition)) {
             updatePlayerPosition(newPosition);
             checkWinCondition();
             }
            }
            
            // Function to check if the move is valid
            function isValidMove(position) {
             return maze[position.row] && maze[position.row][position.col] === 0;
            }
            
            // Function to update the player's position
            function updatePlayerPosition(position) {
             playerPosition = position;
             const playerElement = document.getElementById('player');
             const cellWidth = document.getElementById('gameArea').clientWidth / mazeWidth;
             const cellHeight = document.getElementById('gameArea').clientHeight / mazeHeight;
             playerElement.style.top = `${"$"}{position.row * cellHeight}px`;
             playerElement.style.left = `${"$"}{position.col * cellWidth}px`;
            }
            
            // Function to check win condition
            function checkWinCondition() {
             if (playerPosition.row === endPosition.row && playerPosition.col === endPosition.col) {
             clearInterval(timerInterval);
             gameRunning = false;
             alert('Congratulations! You have completed the maze.');
             }
            }
            
            // Function to start the game
            function startGame() {
             if (gameRunning) return; // Prevent restarting the game if it's already running
             gameRunning = true;
             timerInterval = setInterval(() => {
             timeElapsed++;
             document.getElementById('timer').textContent = formatTime(timeElapsed);
             }, 1000);
            }
            
            // Function to format time from seconds to MM:SS format
            function formatTime(seconds) {
             const minutes = Math.floor(seconds / 60);
             const remainingSeconds = seconds % 60;
             return `${"$"}{padZero(minutes)}:${"$"}{padZero(remainingSeconds)}`;
            }
            
            // Helper function to pad time values with zero
            function padZero(number) {
             return number.toString().padStart(2, '0');
            }
            
            // Function to restart the game
            function restartGame() {
             if (!gameRunning) startGame(); // Ensure game starts if not already running when reset
             clearInterval(timerInterval);
             timeElapsed = 0;
             document.getElementById('timer').textContent = '00:00';
             playerPosition = {...startPosition};
             placePlayer();
             gameRunning = false;
            }
            
            // Initialize the game when the window loads
            window.onload = initializeGame;
        """.trimIndent()
        val result = IterativePatchUtil.patch(source, patch)
        Assertions.assertEquals(
            expected.replace("\r\n", "\n").replace("[ \\t]{1,}".toRegex(), " "),
            result.replace("\r\n", "\n").replace("[ \\t]{1,}".toRegex(), " ")
        )
    }
    @Test
    fun testFromData3() {
        val source = """
package com.simiacryptus.diff

import org.apache.commons.text.similarity.LevenshteinDistance

object IterativePatchUtil {

    enum class LineType { CONTEXT, ADD, DELETE }
    class LineRecord(
        val index: Int,
        val line: String,
        var previousLine: LineRecord? = null,
        var nextLine: LineRecord? = null,
        var matchingLine: LineRecord? = null,
        var type: LineType = LineType.CONTEXT
    ) {
        override fun toString(): String {
            val sb = StringBuilder()
            when (type) {
                LineType.CONTEXT -> sb.append(" ")
                LineType.ADD -> sb.append("+")
                LineType.DELETE -> sb.append("-")
            }
            sb.append(" ")
            sb.append(line)
            return sb.toString()
        }
    }

    /**
     * Applies a patch to the given source text.
     * @param source The original text.
     * @param patch The patch to apply.
     * @return The text after the patch has been applied.
     */
    fun patch(source: String, patch: String): String {
        // Parse the source and patch texts into lists of line records
        val sourceLines = parseLines(source)
        val patchLines = parsePatchLines(patch)

        // Step 1: Link all unique lines in the source and patch that match exactly
        linkUniqueMatchingLines(sourceLines, patchLines)

        // Step 2: Link all exact matches in the source and patch which are adjacent to established links
        linkAdjacentMatchingLines(sourceLines)

        // Step 3: Establish a distance metric for matches based on Levenshtein distance and distance to established links.
        // Use this to establish the links based on a shortest-first policy and iterate until no more good matches are found.
//        linkByLevenshteinDistance(sourceLines, patchLines)

        // Generate the patched text using the established links
        return generatePatchedTextUsingLinks(sourceLines, patchLines)
    }

    /**
     * Generates the final patched text using the links established between the source and patch lines.
     * @param sourceLines The source lines with established links.
     * @param patchLines The patch lines with established links.
     * @return The final patched text.
     */
    private fun generatePatchedTextUsingLinks(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): String {
        val patchedTextBuilder = StringBuilder()
        val sourceLineBuffer = sourceLines.toMutableList()

        // Add any leading 'add' lines from the patch
        val patchLines = patchLines.toMutableList()
        while (patchLines.firstOrNull()?.type == LineType.ADD) {
            patchedTextBuilder.appendLine(patchLines.removeFirst().line)
        }

        // Process the rest of the lines
        while (sourceLineBuffer.isNotEmpty()) {
            // Copy all lines until the next matched line
            val codeLine = sourceLineBuffer.removeFirst()
            when {
                codeLine.matchingLine == null -> patchedTextBuilder.appendLine(codeLine.line)
                codeLine.matchingLine!!.type == LineType.DELETE -> null // Skip adding the line
                codeLine.matchingLine!!.type == LineType.CONTEXT -> patchedTextBuilder.appendLine(codeLine.line)
                codeLine.matchingLine!!.type == LineType.ADD -> throw IllegalStateException("ADD line is matched to source line")
            }

            // Add lines marked as ADD in the patch following the current matched line
            var nextPatchLine = codeLine.matchingLine?.nextLine
            while (nextPatchLine != null && nextPatchLine.matchingLine == null) {
                when(nextPatchLine.type) {
                    LineType.ADD -> patchedTextBuilder.appendLine(nextPatchLine.line)
                    LineType.CONTEXT -> patchedTextBuilder.appendLine(nextPatchLine.line)
                    LineType.DELETE -> null // Skip adding the line
                }
                nextPatchLine = nextPatchLine.nextLine
            }
        }
        return patchedTextBuilder.toString().trimEnd()
    }

    /**
     * Links lines between the source and the patch that are unique and match exactly.
     * @param sourceLines The source lines.
     * @param patchLines The patch lines.
     */
    private fun linkUniqueMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
        val sourceLineMap = sourceLines.groupBy { it.line.trim() }
        val patchLineMap = patchLines.filter {
            when (it.type) {
                LineType.ADD -> false // ADD lines are not matched to source lines
                else -> true
            }
        }.groupBy { it.line.trim() }

        sourceLineMap.keys.intersect(patchLineMap.keys).forEach { key ->
            val sourceLine = sourceLineMap[key]?.singleOrNull()
            val patchLine = patchLineMap[key]?.singleOrNull()
            if (sourceLine != null && patchLine != null) {
                sourceLine.matchingLine = patchLine
                patchLine.matchingLine = sourceLine
            }
        }
    }

    /**
     * Links lines that are adjacent to already linked lines and match exactly.
     * @param sourceLines The source lines with some established links.
     */
    private fun linkAdjacentMatchingLines(sourceLines: List<LineRecord>) {
        var foundMatch = true
        while (foundMatch) {
            foundMatch = false
            for (sourceLine in sourceLines) {
                val patchLine = sourceLine.matchingLine ?: continue // Skip if there's no matching line

                // Check the previous line for a potential match
                if (sourceLine.previousLine != null && patchLine.previousLine != null) {
                    val sourcePrev = sourceLine.previousLine!!
                    var patchPrev = patchLine.previousLine!!
                    while (patchPrev.type == LineType.ADD && patchPrev.previousLine != null) {
                        patchPrev = patchPrev.previousLine!!
                    }
                    if (sourcePrev.line.trim() == patchPrev.line.trim() && sourcePrev.matchingLine == null && patchPrev.matchingLine == null) {
                        sourcePrev.matchingLine = patchPrev
                        patchPrev.matchingLine = sourcePrev
                        foundMatch = true
                    }
                }

                // Check the next line for a potential match
                if (sourceLine.nextLine != null && patchLine.nextLine != null) {
                    val sourceNext = sourceLine.nextLine!!
                    var patchNext = patchLine.nextLine!!
                    while (patchNext.type == LineType.ADD && patchNext.nextLine != null) {
                        patchNext = patchNext.nextLine!!
                    }
                    if (sourceNext.line.trim() == patchNext.line.trim() && sourceNext.matchingLine == null && patchNext.matchingLine == null) {
                        sourceNext.matchingLine = patchNext
                        patchNext.matchingLine = sourceNext
                        foundMatch = true
                    }
                }
            }
        }
    }

    /**
     * Establishes links between source and patch lines based on the Levenshtein distance and proximity to already established links.
     * @param sourceLines The source lines.
     * @param patchLines The patch lines.
     */
    private fun linkByLevenshteinDistance(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
        val levenshteinDistance = LevenshteinDistance()
        val maxDistance = (sourceLines.size + patchLines.size) / 10 // Increase max distance to allow more flexibility

        // Iterate over source lines to find potential matches in the patch lines
        for (sourceLine in sourceLines) {
            if (sourceLine.matchingLine != null) continue // Skip lines that already have matches

            var bestMatch: LineRecord? = null
            var bestDistance = Int.MAX_VALUE
            var bestProximity = Int.MAX_VALUE

            for (patchLine in patchLines.filter {
                when (it.type) {
                    LineType.ADD -> false // ADD lines are not matched to source lines
                    else -> true
                }
            }) {
                if (patchLine.matchingLine != null) continue // Skip lines that already have matches

                // Calculate the Levenshtein distance between unmatched source and patch lines
                val distance = levenshteinDistance.apply(sourceLine.line.trim(), patchLine.line.trim())
                if (distance <= maxDistance) {
                    // Consider proximity to established links as a secondary factor
                    val proximity = calculateProximityDistance(sourceLine, patchLine)
                    if (distance < bestDistance || (distance == bestDistance && proximity < bestProximity)) {
                        if (distance < bestDistance) {
                            bestMatch = patchLine
                            bestDistance = distance
                            bestProximity = proximity
                        }
                    }
                }

                // Establish the best match found, if any
                if (bestMatch != null) {
                    sourceLine.matchingLine = bestMatch
                    bestMatch.matchingLine = sourceLine
                }
            }
        }
    }

    /**
     * Calculates the proximity distance between a source line and a patch line based on their distance to the nearest established link.
     * @param sourceLine The source line.
     * @param patchLine The patch line.
     * @return The proximity distance.
     */
    private fun calculateProximityDistance(sourceLine: LineRecord, patchLine: LineRecord): Int {
        // Find the nearest established link in both directions for source and patch lines
        var sourceDistancePrev = 0
        var sourceDistanceNext = 0
        var patchDistancePrev = 0
        var patchDistanceNext = 0

        var currentSourceLine = sourceLine.previousLine
        while (currentSourceLine != null) {
            if (currentSourceLine.matchingLine != null) break
            sourceDistancePrev++
            currentSourceLine = currentSourceLine.previousLine
        }

        currentSourceLine = sourceLine.nextLine
        while (currentSourceLine != null) {
            if (currentSourceLine.matchingLine != null) break
            sourceDistanceNext++
            currentSourceLine = currentSourceLine.nextLine
        }

        var currentPatchLine = patchLine.previousLine
        while (currentPatchLine != null) {
            if (currentPatchLine.matchingLine != null) break
            patchDistancePrev++
            currentPatchLine = currentPatchLine.previousLine
        }

        currentPatchLine = patchLine.nextLine
        while (currentPatchLine != null) {
            if (currentPatchLine.matchingLine != null) break
            patchDistanceNext++
            currentPatchLine = currentPatchLine.nextLine
        }

        // Calculate the total proximity distance as the sum of minimum distances in each direction
        return minOf(sourceDistancePrev, patchDistancePrev) + minOf(sourceDistanceNext, patchDistanceNext)
    }

    /**
     * Parses the given text into a list of line records.
     * @param text The text to parse.
     * @return The list of line records.
     */
    private fun parseLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
        LineRecord(index, line)
    })

    /**
     * Sets the previous and next line links for a list of line records.
     * @param list The list of line records.
     * @return The list with links set.
     */
    private fun setLinks(list: List<LineRecord>): List<LineRecord> {
        for (i in 0 until list.size) {
            list[i].previousLine = if (i > 0) list[i - 1] else null
            list[i].nextLine = if (i < list.size - 1) list[i + 1] else null
        }
        return list
    }

    /**
     * Parses the patch text into a list of line records, identifying the type of each line (ADD, DELETE, CONTEXT).
     * @param text The patch text to parse.
     * @return The list of line records with types set.
     */
    private fun parsePatchLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
        LineRecord(
            index = index, line = line.let {
                when {
                    it.trimStart().startsWith("+") -> it.trimStart().substring(1)
                    it.trimStart().startsWith("-") -> it.trimStart().substring(1)
                    else -> it
                }
            }, type = when {
                line.startsWith("+") -> LineType.ADD
                line.startsWith("-") -> LineType.DELETE
                else -> LineType.CONTEXT
            }
        )
    })

}

        """.trimIndent()
        @Language("TEXT") val patch = """
package com.simiacryptus.diff

import org.apache.commons.text.similarity.LevenshteinDistance

object IterativePatchUtil {

    enum class LineType { CONTEXT, ADD, DELETE }
    class LineRecord(
        val index: Int,
        val line: String,
        var previousLine: LineRecord? = null,
        var nextLine: LineRecord? = null,
        var matchingLine: LineRecord? = null,
        var type: LineType = LineType.CONTEXT
    ) {
        override fun toString(): String {
            val sb = StringBuilder()
            when (type) {
                LineType.CONTEXT -> sb.append(" ")
                LineType.ADD -> sb.append("+")
                LineType.DELETE -> sb.append("-")
            }
            sb.append(" ")
            sb.append(line)
            return sb.toString()
        }
    }

+   /**
+    * Normalizes a line by removing all whitespace.
+    * @param line The line to normalize.
+    * @return The normalized line.
+    */
+   private fun normalizeLine(line: String): String {
+       return line.replace("\\s".toRegex(), "")
+   }

    /**
     * Applies a patch to the given source text.
     * @param source The original text.
     * @param patch The patch to apply.
     * @return The text after the patch has been applied.
     */
    fun patch(source: String, patch: String): String {
        // Parse the source and patch texts into lists of line records
        val sourceLines = parseLines(source)
        val patchLines = parsePatchLines(patch)

        // Step 1: Link all unique lines in the source and patch that match exactly
        linkUniqueMatchingLines(sourceLines, patchLines)

        // Step 2: Link all exact matches in the source and patch which are adjacent to established links
        linkAdjacentMatchingLines(sourceLines)

        // Step 3: Establish a distance metric for matches based on Levenshtein distance and distance to established links.
        // Use this to establish the links based on a shortest-first policy and iterate until no more good matches are found.
//        linkByLevenshteinDistance(sourceLines, patchLines)

        // Generate the patched text using the established links
        return generatePatchedTextUsingLinks(sourceLines, patchLines)
    }

    /**
     * Generates the final patched text using the links established between the source and patch lines.
     * @param sourceLines The source lines with established links.
     * @param patchLines The patch lines with established links.
     * @return The final patched text.
     */
    private fun generatePatchedTextUsingLinks(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): String {
        val patchedTextBuilder = StringBuilder()
        val sourceLineBuffer = sourceLines.toMutableList()

        // Add any leading 'add' lines from the patch
        val patchLines = patchLines.toMutableList()
        while (patchLines.firstOrNull()?.type == LineType.ADD) {
            patchedTextBuilder.appendLine(patchLines.removeFirst().line)
        }

        // Process the rest of the lines
        while (sourceLineBuffer.isNotEmpty()) {
            // Copy all lines until the next matched line
            val codeLine = sourceLineBuffer.removeFirst()
            when {
                codeLine.matchingLine == null -> patchedTextBuilder.appendLine(codeLine.line)
                codeLine.matchingLine!!.type == LineType.DELETE -> null // Skip adding the line
                codeLine.matchingLine!!.type == LineType.CONTEXT -> patchedTextBuilder.appendLine(codeLine.line)
                codeLine.matchingLine!!.type == LineType.ADD -> throw IllegalStateException("ADD line is matched to source line")
            }

            // Add lines marked as ADD in the patch following the current matched line
            var nextPatchLine = codeLine.matchingLine?.nextLine
            while (nextPatchLine != null && nextPatchLine.matchingLine == null) {
                when(nextPatchLine.type) {
                    LineType.ADD -> patchedTextBuilder.appendLine(nextPatchLine.line)
                    LineType.CONTEXT -> patchedTextBuilder.appendLine(nextPatchLine.line)
                    LineType.DELETE -> null // Skip adding the line
                }
                nextPatchLine = nextPatchLine.nextLine
            }
        }
        return patchedTextBuilder.toString().trimEnd()
    }

    /**
     * Links lines between the source and the patch that are unique and match exactly.
     * @param sourceLines The source lines.
     * @param patchLines The patch lines.
     */
    private fun linkUniqueMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
        val sourceLineMap = sourceLines.groupBy { normalizeLine(it.line) }
        val patchLineMap = patchLines.filter {
            when (it.type) {
                LineType.ADD -> false // ADD lines are not matched to source lines
                else -> true
            }
        }.groupBy { normalizeLine(it.line) }

        sourceLineMap.keys.intersect(patchLineMap.keys).forEach { key ->
            val sourceLine = sourceLineMap[key]?.singleOrNull()
            val patchLine = patchLineMap[key]?.singleOrNull()
            if (sourceLine != null && patchLine != null) {
                sourceLine.matchingLine = patchLine
                patchLine.matchingLine = sourceLine
            }
        }
    }

    /**
     * Links lines that are adjacent to already linked lines and match exactly.
     * @param sourceLines The source lines with some established links.
     */
    private fun linkAdjacentMatchingLines(sourceLines: List<LineRecord>) {
        var foundMatch = true
        while (foundMatch) {
            foundMatch = false
            for (sourceLine in sourceLines) {
                val patchLine = sourceLine.matchingLine ?: continue // Skip if there's no matching line

                // Check the previous line for a potential match
                if (sourceLine.previousLine != null && patchLine.previousLine != null) {
                    val sourcePrev = sourceLine.previousLine!!
                    var patchPrev = patchLine.previousLine!!
                    while (patchPrev.type == LineType.ADD && patchPrev.previousLine != null) {
                        patchPrev = patchPrev.previousLine!!
                    }
                    if (normalizeLine(sourcePrev.line) == normalizeLine(patchPrev.line) && sourcePrev.matchingLine == null && patchPrev.matchingLine == null) {
                        sourcePrev.matchingLine = patchPrev
                        patchPrev.matchingLine = sourcePrev
                        foundMatch = true
                    }
                }

                // Check the next line for a potential match
                if (sourceLine.nextLine != null && patchLine.nextLine != null) {
                    val sourceNext = sourceLine.nextLine!!
                    var patchNext = patchLine.nextLine!!
                    while (patchNext.type == LineType.ADD && patchNext.nextLine != null) {
                        patchNext = patchNext.nextLine!!
                    }
                    if (normalizeLine(sourceNext.line) == normalizeLine(patchNext.line) && sourceNext.matchingLine == null && patchNext.matchingLine == null) {
                        sourceNext.matchingLine = patchNext
                        patchNext.matchingLine = sourceNext
                        foundMatch = true
                    }
                }
            }
        }
    }

    /**
     * Establishes links between source and patch lines based on the Levenshtein distance and proximity to already established links.
     * @param sourceLines The source lines.
     * @param patchLines The patch lines.
     */
    private fun linkByLevenshteinDistance(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
        val levenshteinDistance = LevenshteinDistance()
        val maxDistance = (sourceLines.size + patchLines.size) / 10 // Increase max distance to allow more flexibility

        // Iterate over source lines to find potential matches in the patch lines
        for (sourceLine in sourceLines) {
            if (sourceLine.matchingLine != null) continue // Skip lines that already have matches

            var bestMatch: LineRecord? = null
            var bestDistance = Int.MAX_VALUE
            var bestProximity = Int.MAX_VALUE

            for (patchLine in patchLines.filter {
                when (it.type) {
                    LineType.ADD -> false // ADD lines are not matched to source lines
                    else -> true
                }
            }) {
                if (patchLine.matchingLine != null) continue // Skip lines that already have matches

                // Calculate the Levenshtein distance between unmatched source and patch lines
                val distance = levenshteinDistance.apply(normalizeLine(sourceLine.line), normalizeLine(patchLine.line))
                if (distance <= maxDistance) {
                    // Consider proximity to established links as a secondary factor
                    val proximity = calculateProximityDistance(sourceLine, patchLine)
                    if (distance < bestDistance || (distance == bestDistance && proximity < bestProximity)) {
                        if (distance < bestDistance) {
                            bestMatch = patchLine
                            bestDistance = distance
                            bestProximity = proximity
                        }
                    }
                }

                // Establish the best match found, if any
                if (bestMatch != null) {
                    sourceLine.matchingLine = bestMatch
                    bestMatch.matchingLine = sourceLine
                }
            }
        }
    }

    /**
     * Calculates the proximity distance between a source line and a patch line based on their distance to the nearest established link.
     * @param sourceLine The source line.
     * @param patchLine The patch line.
     * @return The proximity distance.
     */
    private fun calculateProximityDistance(sourceLine: LineRecord, patchLine: LineRecord): Int {
        // Find the nearest established link in both directions for source and patch lines
        var sourceDistancePrev = 0
        var sourceDistanceNext = 0
        var patchDistancePrev = 0
        var patchDistanceNext = 0

        var currentSourceLine = sourceLine.previousLine
        while (currentSourceLine != null) {
            if (currentSourceLine.matchingLine != null) break
            sourceDistancePrev++
            currentSourceLine = currentSourceLine.previousLine
        }

        currentSourceLine = sourceLine.nextLine
        while (currentSourceLine != null) {
            if (currentSourceLine.matchingLine != null) break
            sourceDistanceNext++
            currentSourceLine = currentSourceLine.nextLine
        }

        var currentPatchLine = patchLine.previousLine
        while (currentPatchLine != null) {
            if (currentPatchLine.matchingLine != null) break
            patchDistancePrev++
            currentPatchLine = currentPatchLine.previousLine
        }

        currentPatchLine = patchLine.nextLine
        while (currentPatchLine != null) {
            if (currentPatchLine.matchingLine != null) break
            patchDistanceNext++
            currentPatchLine = currentPatchLine.nextLine
        }

        // Calculate the total proximity distance as the sum of minimum distances in each direction
        return minOf(sourceDistancePrev, patchDistancePrev) + minOf(sourceDistanceNext, patchDistanceNext)
    }

    /**
     * Parses the given text into a list of line records.
     * @param text The text to parse.
     * @return The list of line records.
     */
    private fun parseLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
        LineRecord(index, line)
    })

    /**
     * Sets the previous and next line links for a list of line records.
     * @param list The list of line records.
     * @return The list with links set.
     */
    private fun setLinks(list: List<LineRecord>): List<LineRecord> {
        for (i in 0 until list.size) {
            list[i].previousLine = if (i > 0) list[i - 1] else null
            list[i].nextLine = if (i < list.size - 1) list[i + 1] else null
        }
        return list
    }

    /**
     * Parses the patch text into a list of line records, identifying the type of each line (ADD, DELETE, CONTEXT).
     * @param text The patch text to parse.
     * @return The list of line records with types set.
     */
    private fun parsePatchLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
        LineRecord(
            index = index, line = line.let {
                when {
                    it.trimStart().startsWith("+") -> it.trimStart().substring(1)
                    it.trimStart().startsWith("-") -> it.trimStart().substring(1)
                    else -> it
                }
            }, type = when {
                line.startsWith("+") -> LineType.ADD
                line.startsWith("-") -> LineType.DELETE
                else -> LineType.CONTEXT
            }
        )
    })

}

        """.trimIndent()
        val expected = """
package com.simiacryptus.diff

import org.apache.commons.text.similarity.LevenshteinDistance

object IterativePatchUtil {

 enum class LineType { CONTEXT, ADD, DELETE }
 class LineRecord(
 val index: Int,
 val line: String,
 var previousLine: LineRecord? = null,
 var nextLine: LineRecord? = null,
 var matchingLine: LineRecord? = null,
 var type: LineType = LineType.CONTEXT
 ) {
 override fun toString(): String {
 val sb = StringBuilder()
 when (type) {
 LineType.CONTEXT -> sb.append(" ")
 LineType.ADD -> sb.append("+")
 LineType.DELETE -> sb.append("-")
 }
 sb.append(" ")
 sb.append(line)
 return sb.toString()
 }
 }

 /**
 * Normalizes a line by removing all whitespace.
 * @param line The line to normalize.
 * @return The normalized line.
 */
 private fun normalizeLine(line: String): String {
 return line.replace("\\s".toRegex(), "")
 }

 /**
 * Applies a patch to the given source text.
 * @param source The original text.
 * @param patch The patch to apply.
 * @return The text after the patch has been applied.
 */
 fun patch(source: String, patch: String): String {
 // Parse the source and patch texts into lists of line records
 val sourceLines = parseLines(source)
 val patchLines = parsePatchLines(patch)

 // Step 1: Link all unique lines in the source and patch that match exactly
 linkUniqueMatchingLines(sourceLines, patchLines)

 // Step 2: Link all exact matches in the source and patch which are adjacent to established links
 linkAdjacentMatchingLines(sourceLines)

 // Step 3: Establish a distance metric for matches based on Levenshtein distance and distance to established links.
 // Use this to establish the links based on a shortest-first policy and iterate until no more good matches are found.
// linkByLevenshteinDistance(sourceLines, patchLines)

 // Generate the patched text using the established links
 return generatePatchedTextUsingLinks(sourceLines, patchLines)
 }

 /**
 * Generates the final patched text using the links established between the source and patch lines.
 * @param sourceLines The source lines with established links.
 * @param patchLines The patch lines with established links.
 * @return The final patched text.
 */
 private fun generatePatchedTextUsingLinks(sourceLines: List<LineRecord>, patchLines: List<LineRecord>): String {
 val patchedTextBuilder = StringBuilder()
 val sourceLineBuffer = sourceLines.toMutableList()

 // Add any leading 'add' lines from the patch
 val patchLines = patchLines.toMutableList()
 while (patchLines.firstOrNull()?.type == LineType.ADD) {
 patchedTextBuilder.appendLine(patchLines.removeFirst().line)
 }

 // Process the rest of the lines
 while (sourceLineBuffer.isNotEmpty()) {
 // Copy all lines until the next matched line
 val codeLine = sourceLineBuffer.removeFirst()
 when {
 codeLine.matchingLine == null -> patchedTextBuilder.appendLine(codeLine.line)
 codeLine.matchingLine!!.type == LineType.DELETE -> null // Skip adding the line
 codeLine.matchingLine!!.type == LineType.CONTEXT -> patchedTextBuilder.appendLine(codeLine.line)
 codeLine.matchingLine!!.type == LineType.ADD -> throw IllegalStateException("ADD line is matched to source line")
 }

 // Add lines marked as ADD in the patch following the current matched line
 var nextPatchLine = codeLine.matchingLine?.nextLine
 while (nextPatchLine != null && nextPatchLine.matchingLine == null) {
 when(nextPatchLine.type) {
 LineType.ADD -> patchedTextBuilder.appendLine(nextPatchLine.line)
 LineType.CONTEXT -> patchedTextBuilder.appendLine(nextPatchLine.line)
 LineType.DELETE -> null // Skip adding the line
 }
 nextPatchLine = nextPatchLine.nextLine
 }
 }
 return patchedTextBuilder.toString().trimEnd()
 }

 /**
 * Links lines between the source and the patch that are unique and match exactly.
 * @param sourceLines The source lines.
 * @param patchLines The patch lines.
 */
 private fun linkUniqueMatchingLines(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
 val sourceLineMap = sourceLines.groupBy { normalizeLine(it.line) }
 val patchLineMap = patchLines.filter {
 when (it.type) {
 LineType.ADD -> false // ADD lines are not matched to source lines
 else -> true
 }
 }.groupBy { normalizeLine(it.line) }

 sourceLineMap.keys.intersect(patchLineMap.keys).forEach { key ->
 val sourceLine = sourceLineMap[key]?.singleOrNull()
 val patchLine = patchLineMap[key]?.singleOrNull()
 if (sourceLine != null && patchLine != null) {
 sourceLine.matchingLine = patchLine
 patchLine.matchingLine = sourceLine
 }
 }
 }

 /**
 * Links lines that are adjacent to already linked lines and match exactly.
 * @param sourceLines The source lines with some established links.
 */
 private fun linkAdjacentMatchingLines(sourceLines: List<LineRecord>) {
 var foundMatch = true
 while (foundMatch) {
 foundMatch = false
 for (sourceLine in sourceLines) {
 val patchLine = sourceLine.matchingLine ?: continue // Skip if there's no matching line

 // Check the previous line for a potential match
 if (sourceLine.previousLine != null && patchLine.previousLine != null) {
 val sourcePrev = sourceLine.previousLine!!
 var patchPrev = patchLine.previousLine!!
 while (patchPrev.type == LineType.ADD && patchPrev.previousLine != null) {
 patchPrev = patchPrev.previousLine!!
 }
 if (normalizeLine(sourcePrev.line) == normalizeLine(patchPrev.line) && sourcePrev.matchingLine == null && patchPrev.matchingLine == null) {
 sourcePrev.matchingLine = patchPrev
 patchPrev.matchingLine = sourcePrev
 foundMatch = true
 }
 }

 // Check the next line for a potential match
 if (sourceLine.nextLine != null && patchLine.nextLine != null) {
 val sourceNext = sourceLine.nextLine!!
 var patchNext = patchLine.nextLine!!
 while (patchNext.type == LineType.ADD && patchNext.nextLine != null) {
 patchNext = patchNext.nextLine!!
 }
 if (normalizeLine(sourceNext.line) == normalizeLine(patchNext.line) && sourceNext.matchingLine == null && patchNext.matchingLine == null) {
 sourceNext.matchingLine = patchNext
 patchNext.matchingLine = sourceNext
 foundMatch = true
 }
 }
 }
 }
 }

 /**
 * Establishes links between source and patch lines based on the Levenshtein distance and proximity to already established links.
 * @param sourceLines The source lines.
 * @param patchLines The patch lines.
 */
 private fun linkByLevenshteinDistance(sourceLines: List<LineRecord>, patchLines: List<LineRecord>) {
 val levenshteinDistance = LevenshteinDistance()
 val maxDistance = (sourceLines.size + patchLines.size) / 10 // Increase max distance to allow more flexibility

 // Iterate over source lines to find potential matches in the patch lines
 for (sourceLine in sourceLines) {
 if (sourceLine.matchingLine != null) continue // Skip lines that already have matches

 var bestMatch: LineRecord? = null
 var bestDistance = Int.MAX_VALUE
 var bestProximity = Int.MAX_VALUE

 for (patchLine in patchLines.filter {
 when (it.type) {
 LineType.ADD -> false // ADD lines are not matched to source lines
 else -> true
 }
 }) {
 if (patchLine.matchingLine != null) continue // Skip lines that already have matches

 // Calculate the Levenshtein distance between unmatched source and patch lines
 val distance = levenshteinDistance.apply(normalizeLine(sourceLine.line), normalizeLine(patchLine.line))
 if (distance <= maxDistance) {
 // Consider proximity to established links as a secondary factor
 val proximity = calculateProximityDistance(sourceLine, patchLine)
 if (distance < bestDistance || (distance == bestDistance && proximity < bestProximity)) {
 if (distance < bestDistance) {
 bestMatch = patchLine
 bestDistance = distance
 bestProximity = proximity
 }
 }
 }

 // Establish the best match found, if any
 if (bestMatch != null) {
 sourceLine.matchingLine = bestMatch
 bestMatch.matchingLine = sourceLine
 }
 }
 }
 }

 /**
 * Calculates the proximity distance between a source line and a patch line based on their distance to the nearest established link.
 * @param sourceLine The source line.
 * @param patchLine The patch line.
 * @return The proximity distance.
 */
 private fun calculateProximityDistance(sourceLine: LineRecord, patchLine: LineRecord): Int {
 // Find the nearest established link in both directions for source and patch lines
 var sourceDistancePrev = 0
 var sourceDistanceNext = 0
 var patchDistancePrev = 0
 var patchDistanceNext = 0

 var currentSourceLine = sourceLine.previousLine
 while (currentSourceLine != null) {
 if (currentSourceLine.matchingLine != null) break
 sourceDistancePrev++
 currentSourceLine = currentSourceLine.previousLine
 }

 currentSourceLine = sourceLine.nextLine
 while (currentSourceLine != null) {
 if (currentSourceLine.matchingLine != null) break
 sourceDistanceNext++
 currentSourceLine = currentSourceLine.nextLine
 }

 var currentPatchLine = patchLine.previousLine
 while (currentPatchLine != null) {
 if (currentPatchLine.matchingLine != null) break
 patchDistancePrev++
 currentPatchLine = currentPatchLine.previousLine
 }

 currentPatchLine = patchLine.nextLine
 while (currentPatchLine != null) {
 if (currentPatchLine.matchingLine != null) break
 patchDistanceNext++
 currentPatchLine = currentPatchLine.nextLine
 }

 // Calculate the total proximity distance as the sum of minimum distances in each direction
 return minOf(sourceDistancePrev, patchDistancePrev) + minOf(sourceDistanceNext, patchDistanceNext)
 }

 /**
 * Parses the given text into a list of line records.
 * @param text The text to parse.
 * @return The list of line records.
 */
 private fun parseLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
 LineRecord(index, line)
 })

 /**
 * Sets the previous and next line links for a list of line records.
 * @param list The list of line records.
 * @return The list with links set.
 */
 private fun setLinks(list: List<LineRecord>): List<LineRecord> {
 for (i in 0 until list.size) {
 list[i].previousLine = if (i > 0) list[i - 1] else null
 list[i].nextLine = if (i < list.size - 1) list[i + 1] else null
 }
 return list
 }

 /**
 * Parses the patch text into a list of line records, identifying the type of each line (ADD, DELETE, CONTEXT).
 * @param text The patch text to parse.
 * @return The list of line records with types set.
 */
 private fun parsePatchLines(text: String) = setLinks(text.lines().mapIndexed { index, line ->
 LineRecord(
 index = index, line = line.let {
 when {
 it.trimStart().startsWith("+") -> it.trimStart().substring(1)
 it.trimStart().startsWith("-") -> it.trimStart().substring(1)
 else -> it
 }
 }, type = when {
 line.startsWith("+") -> LineType.ADD
 line.startsWith("-") -> LineType.DELETE
 else -> LineType.CONTEXT
 }
 )
 })

}
        """.trimIndent()
        val result = IterativePatchUtil.patch(source, patch)
        Assertions.assertEquals(
            expected.replace("\r\n", "\n").replace("[ \\t]{1,}".toRegex(), " "),
            result.replace("\r\n", "\n").replace("[ \\t]{1,}".toRegex(), " ")
        )
    }
}