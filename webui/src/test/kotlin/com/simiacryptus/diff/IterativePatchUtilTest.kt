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

/*

    @Test
    fun testFromData2() {
        @Language("KT") val source = """
type RGB = {
    r: number;
    g: number;
    b: number;
};

export class ColorMixer {
    // ... other methods ...

    public static hexToRgb(hex: string): RGB {
        // ... existing implementation ...
    }

    public static convertHexToRgb(hex: string): RGB {
        return this.hexToRgb(hex);
    }

    private static hexToRgb(hex: string): RGB {
        // ... other methods ...
    public
        mixColors(...colors
    :
        string[]
    ):
        string
        {
            // Implementation of color mixing logic
            // This is a placeholder implementation
            return colors[0]; // Return the first color for now
        }

        // ... existing methods ...
    }

    private static clamp(value: number): number {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static rgbToHex(rgb: RGB): string {
        return `#${'$'}{ColorMixer.clamp(rgb.r).toString(16).padStart(2, '0')}${'$'}{ColorMixer.clamp(rgb.g).toString(16).padStart(2, '0')}${'$'}{ColorMixer.clamp(rgb.b).toString(16).padStart(2, '0')}`;
    }

    // ... other methods ...
}

// ... other methods ...
}
const result = "/^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})${'$'}/i".exec(hex);
return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
} : {r: 0, g: 0, b: 0};
}

static
mixAdditive(...colors
:
string[]
):
string
{
    if (colors.length === 0) return '#000000';

    const mixed: RGB = colors.reduce((acc, color) => {
        const rgb = ColorMixer.hexToRgb(color);
        return {
            r: acc.r + rgb.r,
            g: acc.g + rgb.g,
            b: acc.b + rgb.b
        };
    }, {r: 0, g: 0, b: 0});

    const count = colors.length;
    mixed.r = ColorMixer.clamp(mixed.r / count);
    mixed.g = ColorMixer.clamp(mixed.g / count);
    mixed.b = ColorMixer.clamp(mixed.b / count);

    return ColorMixer.rgbToHex(mixed);
}

static
mixSubtractive(...colors
:
string[]
):
string
{
    if (colors.length === 0) return '#FFFFFF';

    const mixed: RGB = colors.reduce((acc, color) => {
        const rgb = ColorMixer.hexToRgb(color);
        return {
            r: acc.r * (rgb.r / 255),
            g: acc.g * (rgb.g / 255),
            b: acc.b * (rgb.b / 255)
        };
    }, {r: 255, g: 255, b: 255});

    mixed.r = ColorMixer.clamp(mixed.r);
    mixed.g = ColorMixer.clamp(mixed.g);
    mixed.b = ColorMixer.clamp(mixed.b);

    return ColorMixer.rgbToHex(mixed);
}

static
getComplementaryColor(color
:
string
):
string
{
    const rgb = ColorMixer.hexToRgb(color);
    const complementary: RGB = {
        r: 255 - rgb.r,
        g: 255 - rgb.g,
        b: 255 - rgb.b
    };
    return ColorMixer.rgbToHex(complementary);
}

static
getLightness(color
:
string
):
number
{
    const rgb = ColorMixer.hexToRgb(color);
    return (Math.max(rgb.r, rgb.g, rgb.b) + Math.min(rgb.r, rgb.g, rgb.b)) / (2 * 255);
}

static
adjustLightness(color
:
string, amount
:
number
):
string
{
    const rgb = ColorMixer.hexToRgb(color);
    const hsl = ColorMixer.rgbToHsl(rgb);
    hsl.l = Math.max(0, Math.min(1, hsl.l + amount));
    return ColorMixer.rgbToHex(ColorMixer.hslToRgb(hsl));
}

private static
rgbToHsl(rgb
:
RGB
):
{
    number, s
:
    number, l
:
    number
}
{
    const r = rgb.r / 255;
    const g = rgb.g / 255;
    const b = rgb.b / 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h: number;
    const l = (max + min) / 2;

    if (max === min) {
        h = 0; // Achromatic
    } else {
        h = s = 0;
    }
else
    {
        const d = max - min;
        const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch (max) {
            case r:
                h = (g - b) / d + (g < b ? 6 : 0);
                break;
            case g:
                h = (b - r) / d + 2;
                break;
            case b:
                h = (r - g) / d + 4;
                break;
            default:
                h = 0; // This should never happen, but it satisfies the type checker
            case r:
                h = (g - b) / d + (g < b ? 6 : 0);
                break;
            case g:
                h = (b - r) / d + 2;
                break;
            case b:
                h = (r - g) / d + 4;
                break;
        }
        h = h / 6;
    }

    return {h, s, l};
}

private static
hslToRgb(hsl
:
{
    number, s
:
    number, l
:
    number
}
):
RGB
{
    const {h, s, l} = hsl;
    let r, g, b;

    if (s === 0) {
        r = g = b = l;
    } else {
        const hue2rgb = (p: number, q: number, t: number) => {
            if (t < 0) t += 1;
            if (t > 1) t -= 1;
            if (t < 1 / 6) return p + (q - p) * 6 * t;
            if (t < 1 / 2) return q;
            if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
            return p;
        };

        const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        const p = 2 * l - q;
        r = hue2rgb(p, q, h + 1 / 3);
        g = hue2rgb(p, q, h);
        b = hue2rgb(p, q, h - 1 / 3);
    }

    return {
        r: Math.round(r * 255),
        g: Math.round(g * 255),
        b: Math.round(b * 255)
    };
}
}            
        """.trimIndent()
        val patch = """
            | export class ColorMixer {
            |+    public mixColors(...colors: string[]): string {
            |+        if (colors.length === 0) return '#000000';
            |+        if (colors.length === 1) return colors[0];
            |+
            |+        const rgbColors = colors.map(color => this.hexToRgb(color));
            |+        const mixedRgb = rgbColors.reduce((acc, rgb) => ({
            |+            r: acc.r + rgb.r,
            |+            g: acc.g + rgb.g,
            |+            b: acc.b + rgb.b
            |+        }));
            |+
            |+        const avgRgb = {
            |+            r: Math.round(mixedRgb.r / colors.length),
            |+            g: Math.round(mixedRgb.g / colors.length),
            |+            b: Math.round(mixedRgb.b / colors.length)
            |+        };
            |+
            |+        return this.rgbToHex(avgRgb);
            |+    }
            |
            |     public static hexToRgb(hex: string): RGB {
            |         // Existing implementation...
            |     }
            |
            |     private static rgbToHex(rgb: RGB): string {
            |         // Existing implementation...
            |     }
            |
            |     // Other existing methods...
            | }
        """.trimMargin()
        val expected = """
type RGB = {
    r: number;
    g: number;
    b: number;
};

export class ColorMixer {
    public mixColors(...colors: string[]): string {
        if (colors.length === 0) return '#000000';
        if (colors.length === 1) return colors[0];

        const rgbColors = colors.map(color => this.hexToRgb(color));
        const mixedRgb = rgbColors.reduce((acc, rgb) => ({
            r: acc.r + rgb.r,
            g: acc.g + rgb.g,
            b: acc.b + rgb.b
        }));
        const avgRgb = {
            r: Math.round(mixedRgb.r / colors.length),
            g: Math.round(mixedRgb.g / colors.length),
            b: Math.round(mixedRgb.b / colors.length)
        };
        return this.rgbToHex(avgRgb);
    }
    // ... other methods ...

    public static hexToRgb(hex: string): RGB {
        // ... existing implementation ...
    }

    public static convertHexToRgb(hex: string): RGB {
        return this.hexToRgb(hex);
    }

    private static hexToRgb(hex: string): RGB {
        // ... other methods ...
    public
        mixColors(...colors
    :
        string[]
    ):
        string
        {
            // Implementation of color mixing logic
            // This is a placeholder implementation
            return colors[0]; // Return the first color for now
        }

        // ... existing methods ...
    }

    private static clamp(value: number): number {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static rgbToHex(rgb: RGB): string {
        return `#${'$'}{ColorMixer.clamp(rgb.r).toString(16).padStart(2, '0')}${'$'}{ColorMixer.clamp(rgb.g).toString(16).padStart(2, '0')}${'$'}{ColorMixer.clamp(rgb.b).toString(16).padStart(2, '0')}`;
    }

    // ... other methods ...
}

// ... other methods ...
}
const result = "/^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})${'$'}/i".exec(hex);
return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
} : {r: 0, g: 0, b: 0};
}

static
mixAdditive(...colors
:
string[]
):
string
{
    if (colors.length === 0) return '#000000';

    const mixed: RGB = colors.reduce((acc, color) => {
        const rgb = ColorMixer.hexToRgb(color);
        return {
            r: acc.r + rgb.r,
            g: acc.g + rgb.g,
            b: acc.b + rgb.b
        };
    }, {r: 0, g: 0, b: 0});

    const count = colors.length;
    mixed.r = ColorMixer.clamp(mixed.r / count);
    mixed.g = ColorMixer.clamp(mixed.g / count);
    mixed.b = ColorMixer.clamp(mixed.b / count);

    return ColorMixer.rgbToHex(mixed);
}

static
mixSubtractive(...colors
:
string[]
):
string
{
    if (colors.length === 0) return '#FFFFFF';

    const mixed: RGB = colors.reduce((acc, color) => {
        const rgb = ColorMixer.hexToRgb(color);
        return {
            r: acc.r * (rgb.r / 255),
            g: acc.g * (rgb.g / 255),
            b: acc.b * (rgb.b / 255)
        };
    }, {r: 255, g: 255, b: 255});

    mixed.r = ColorMixer.clamp(mixed.r);
    mixed.g = ColorMixer.clamp(mixed.g);
    mixed.b = ColorMixer.clamp(mixed.b);

    return ColorMixer.rgbToHex(mixed);
}

static
getComplementaryColor(color
:
string
):
string
{
    const rgb = ColorMixer.hexToRgb(color);
    const complementary: RGB = {
        r: 255 - rgb.r,
        g: 255 - rgb.g,
        b: 255 - rgb.b
    };
    return ColorMixer.rgbToHex(complementary);
}

static
getLightness(color
:
string
):
number
{
    const rgb = ColorMixer.hexToRgb(color);
    return (Math.max(rgb.r, rgb.g, rgb.b) + Math.min(rgb.r, rgb.g, rgb.b)) / (2 * 255);
}

static
adjustLightness(color
:
string, amount
:
number
):
string
{
    const rgb = ColorMixer.hexToRgb(color);
    const hsl = ColorMixer.rgbToHsl(rgb);
    hsl.l = Math.max(0, Math.min(1, hsl.l + amount));
    return ColorMixer.rgbToHex(ColorMixer.hslToRgb(hsl));
}

private static
rgbToHsl(rgb
:
RGB
):
{
    number, s
:
    number, l
:
    number
}
{
    const r = rgb.r / 255;
    const g = rgb.g / 255;
    const b = rgb.b / 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h: number;
    const l = (max + min) / 2;

    if (max === min) {
        h = 0; // Achromatic
    } else {
        h = s = 0;
    }
else
    {
        const d = max - min;
        const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch (max) {
            case r:
                h = (g - b) / d + (g < b ? 6 : 0);
                break;
            case g:
                h = (b - r) / d + 2;
                break;
            case b:
                h = (r - g) / d + 4;
                break;
            default:
                h = 0; // This should never happen, but it satisfies the type checker
            case r:
                h = (g - b) / d + (g < b ? 6 : 0);
                break;
            case g:
                h = (b - r) / d + 2;
                break;
            case b:
                h = (r - g) / d + 4;
                break;
        }
        h = h / 6;
    }

    return {h, s, l};
}

private static
hslToRgb(hsl
:
{
    number, s
:
    number, l
:
    number
}
):
RGB
{
    const {h, s, l} = hsl;
    let r, g, b;

    if (s === 0) {
        r = g = b = l;
    } else {
        const hue2rgb = (p: number, q: number, t: number) => {
            if (t < 0) t += 1;
            if (t > 1) t -= 1;
            if (t < 1 / 6) return p + (q - p) * 6 * t;
            if (t < 1 / 2) return q;
            if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
            return p;
        };

        const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        const p = 2 * l - q;
        r = hue2rgb(p, q, h + 1 / 3);
        g = hue2rgb(p, q, h);
        b = hue2rgb(p, q, h - 1 / 3);
    }

    return {
        r: Math.round(r * 255),
        g: Math.round(g * 255),
        b: Math.round(b * 255)
    };
}
}
        """.trimIndent()
        val result = IterativePatchUtil.patch(source, patch)
        Assertions.assertEquals(
            expected.replace("\r\n", "\n"),//.replace("\\s{2,}".toRegex(), " "),
            result.replace("\r\n", "\n")//.replace("\\s{2,}".toRegex(), " ")
        )
    }

    @Test
    fun testFromData3() {
        val source = """
import {Scene} from './Scene';

export class Game {
    private currentScene: Scene | null = null;
    private isRunning: boolean = false;
    private lastTimestamp: number = 0;

    constructor(private canvas: HTMLCanvasElement) {
    }

    public start(): void {
        this.isRunning = true;
        this.lastTimestamp = performance.now();
        requestAnimationFrame(this.gameLoop.bind(this));
    }

    public stop(): void {
        this.isRunning = false;
    }

    public setScene(scene: Scene): void {
        this.currentScene = scene;
    }

    private gameLoop(timestamp: number): void {
        if (!this.isRunning) return;

        const deltaTime = (timestamp - this.lastTimestamp) / 1000; // Convert to seconds
        this.lastTimestamp = timestamp;

        this.update(deltaTime);
        this.render();

        requestAnimationFrame(this.gameLoop.bind(this));
    }

    private update(deltaTime: number): void {
        if (this.currentScene) {
            this.currentScene.update(deltaTime);
        }
    }

    private render(): void {
        const ctx = this.canvas.getContext('2d');
        if (!ctx) return;

        // Clear the canvas
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        if (this.currentScene) {
            this.currentScene.render(ctx);
        }
    }
}
        """.trimIndent()
        val patch = """
     constructor(private canvas: HTMLCanvasElement) {
+        this.width = canvas.width;
+        this.height = canvas.height;
     }

+    public width: number;
+    public height: number;
        """.trimIndent()
        val expected = """
import {Scene} from './Scene';

export class Game {
    private currentScene: Scene | null = null;
    private isRunning: boolean = false;
    private lastTimestamp: number = 0;

    constructor(private canvas: HTMLCanvasElement) {
        this.width = canvas.width;
        this.height = canvas.height;
    }

    public width: number;
    public height: number;

    public start(): void {
        this.isRunning = true;
        this.lastTimestamp = performance.now();
        requestAnimationFrame(this.gameLoop.bind(this));
    }

    public stop(): void {
        this.isRunning = false;
    }

    public setScene(scene: Scene): void {
        this.currentScene = scene;
    }

    private gameLoop(timestamp: number): void {
        if (!this.isRunning) return;

        const deltaTime = (timestamp - this.lastTimestamp) / 1000; // Convert to seconds
        this.lastTimestamp = timestamp;

        this.update(deltaTime);
        this.render();

        requestAnimationFrame(this.gameLoop.bind(this));
    }

    private update(deltaTime: number): void {
        if (this.currentScene) {
            this.currentScene.update(deltaTime);
        }
    }

    private render(): void {
        const ctx = this.canvas.getContext('2d');
        if (!ctx) return;

        // Clear the canvas
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        if (this.currentScene) {
            this.currentScene.render(ctx);
        }
    }
}
        """.trimIndent()
        val result = IterativePatchUtil.patch(source, patch)
        Assertions.assertEquals(
            expected.replace("\r\n", "\n"),//.replace("\\s{2,}".toRegex(), " "),
            result.replace("\r\n", "\n")//.replace("\\s{2,}".toRegex(), " ")
        )
    }

    @Test
    fun testFromData4() {
        val source = """
import {ColorMixer} from '@/systems/ColorMixer';

describe('ColorMixer', () => {
    const colorMixer = new ColorMixer();

    test('mixing red and green should result in yellow', () => {
        let colorMixer: ColorMixer;

        beforeEach(() => {
            colorMixer = new ColorMixer();
        });

        test('mixing red and green should produce yellow', () => {
            const result = colorMixer.mixColors('red', 'green');
            expect(result).toBe('red'); // Temporary expectation based on placeholder implementation
        });

        test('mixing blue and yellow should result in green', () => {
            const result = colorMixer.mixColors('blue', 'yellow');
            expect(result).toBe('blue'); // Temporary expectation based on placeholder implementation
        });

        test('mixing red, green, and blue should result in white', () => {
            const result = colorMixer.mixColors('red', 'green', 'blue');
            expect(result).toBe('red'); // Temporary expectation based on placeholder implementation
        });
    });
        """.trimIndent()
        val patch = """
 import {ColorMixer} from '@/systems/ColorMixer';

 describe('ColorMixer', () => {
-    const colorMixer = new ColorMixer();
+    let colorMixer: ColorMixer;

     beforeEach(() => {
         colorMixer = new ColorMixer();
     });

     test('mixing red and green should produce yellow', () => {
         const result = colorMixer.mixColors('red', 'green');
-        expect(result).toBe('red'); // Temporary expectation based on placeholder implementation
+        expect(result).toBe('#ffff00'); // Yellow in hex
     });

     test('mixing blue and yellow should result in green', () => {
         const result = colorMixer.mixColors('blue', 'yellow');
-        expect(result).toBe('blue'); // Temporary expectation based on placeholder implementation
+        expect(result).toBe('#80ff80'); // Light green in hex
     });

     test('mixing red, green, and blue should result in white', () => {
         const result = colorMixer.mixColors('red', 'green', 'blue');
-        expect(result).toBe('red'); // Temporary expectation based on placeholder implementation
+        expect(result).toBe('#ffffff'); // White in hex
     });
 });
        """.trimIndent()
        val result = IterativePatchUtil.patch(source, patch)
        val expected = """
import {ColorMixer} from '@/systems/ColorMixer';

describe('ColorMixer', () => {
    const colorMixer = new ColorMixer();
    let colorMixer: ColorMixer;

    beforeEach(() => {
        colorMixer = new ColorMixer();
    });

    test('mixing red and green should produce yellow', () => {
        const result = colorMixer.mixColors('red', 'green');
        expect(result).toBe('#ffff00'); // Yellow in hex
    });

    test('mixing blue and yellow should result in green', () => {
        const result = colorMixer.mixColors('blue', 'yellow');
        expect(result).toBe('#80ff80'); // Light green in hex
    });

    test('mixing red, green, and blue should result in white', () => {
        const result = colorMixer.mixColors('red', 'green', 'blue');
        expect(result).toBe('#ffffff'); // White in hex
    });
});
        """.trimIndent()
        Assertions.assertEquals(
            expected.replace("\r\n", "\n"),//.replace("\\s{2,}".toRegex(), " "),
            result.replace("\r\n", "\n")//.replace("\\s{2,}".toRegex(), " ")
        )
    }
*/

}