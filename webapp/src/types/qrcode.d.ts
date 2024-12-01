declare module "qrcode-generator" {
    export interface QRCode {
        addData(data: string): void;

        make(): void;
        createImgTag(cellSize?: number, margin?: number): string;

        createDataURL(cellSize?: number, margin?: number): string;

        createSvgTag(cellSize?: number, margin?: number): string;
        createASCII(cellSize?: number, margin?: number): string;
        getModuleCount(): number;
        isDark(row: number, col: number): boolean;
    }

    export interface QRCodeGenerator {
        QRCode: QRCode;
        TypeNumber: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 40;
        ErrorCorrectionLevel: {
            L: string;
            M: string;
            Q: string;
            H: string;
        };

        (typeNumber?: number, errorCorrectionLevel?: string): QRCode;
    }

    const qrcode: QRCodeGenerator;
    export = qrcode;
}