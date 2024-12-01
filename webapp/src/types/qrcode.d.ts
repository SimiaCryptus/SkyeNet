declare module 'qrcode-generator' {
    export interface QRCode {
        addData(data: string): void;

        make(): void;

        createDataURL(cellSize?: number, margin?: number): string;

        createSvgTag(cellSize?: number, margin?: number): string;
    }

    export interface QRCodeGenerator {
        TypeNumber: number;
        ErrorCorrectionLevel: {
            L: string;
            M: string;
            Q: string;
            H: string;
        };

        (type?: number, errorCorrectionLevel?: string): QRCode;
    }

    const qrcode: QRCodeGenerator;
    export = qrcode;
}