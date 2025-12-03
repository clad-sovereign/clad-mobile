import SwiftUI
import AVFoundation

/// View for scanning QR codes containing SS58 addresses
struct QRCodeScannerView: View {
    var viewModel: AccountImportViewModel
    @Environment(\.colorScheme) var colorScheme
    @State private var isTorchOn = false
    @State private var cameraPermissionStatus: AVAuthorizationStatus = .notDetermined

    var body: some View {
        let colors = CladColors.ColorScheme.forScheme(colorScheme)

        ZStack {
            // Camera view
            QRCameraPreview(
                isTorchOn: $isTorchOn,
                onCodeScanned: { code in
                    viewModel.handleScannedCode(code)
                }
            )
            .ignoresSafeArea()

            // Overlay
            VStack {
                Spacer()

                // Scanning frame
                ScannerOverlay(colors: colors)

                Spacer()

                // Instructions and controls
                VStack(spacing: 16) {
                    Text("Position QR code within the frame")
                        .font(CladTypography.bodyMedium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.black.opacity(0.6))
                        .cornerRadius(8)

                    // Validation error
                    if let error = viewModel.validationError {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(colors.error)
                            Text(error)
                                .font(CladTypography.bodyMedium)
                                .foregroundColor(.white)
                        }
                        .padding(12)
                        .background(colors.error.opacity(0.8))
                        .cornerRadius(8)
                    }

                    // Torch toggle
                    Button(action: { isTorchOn.toggle() }) {
                        HStack {
                            Image(systemName: isTorchOn ? "flashlight.on.fill" : "flashlight.off.fill")
                            Text(isTorchOn ? "Turn Off Light" : "Turn On Light")
                        }
                        .font(CladTypography.bodyMedium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.black.opacity(0.6))
                        .cornerRadius(8)
                    }
                }
                .padding(.bottom, 60)
            }

            // Permission denied overlay
            if cameraPermissionStatus == .denied || cameraPermissionStatus == .restricted {
                CameraPermissionDeniedView(colors: colors)
            }
        }
        .navigationTitle("Scan QR Code")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            checkCameraPermission()
        }
    }

    private func checkCameraPermission() {
        cameraPermissionStatus = AVCaptureDevice.authorizationStatus(for: .video)

        if cameraPermissionStatus == .notDetermined {
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    cameraPermissionStatus = granted ? .authorized : .denied
                }
            }
        }
    }
}

/// Camera preview using AVFoundation
struct QRCameraPreview: UIViewRepresentable {
    @Binding var isTorchOn: Bool
    let onCodeScanned: (String) -> Void

    func makeUIView(context: Context) -> QRCameraUIView {
        let view = QRCameraUIView()
        view.onCodeScanned = onCodeScanned
        return view
    }

    func updateUIView(_ uiView: QRCameraUIView, context: Context) {
        uiView.setTorch(on: isTorchOn)
    }
}

/// UIKit view for camera capture
class QRCameraUIView: UIView {
    var onCodeScanned: ((String) -> Void)?

    private var captureSession: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var lastScannedCode: String?
    private var lastScanTime: Date?

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupCamera()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupCamera()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }

    private func setupCamera() {
        let session = AVCaptureSession()
        captureSession = session

        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else { return }

        do {
            let videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)

            if session.canAddInput(videoInput) {
                session.addInput(videoInput)
            }

            let metadataOutput = AVCaptureMetadataOutput()

            if session.canAddOutput(metadataOutput) {
                session.addOutput(metadataOutput)
                metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                metadataOutput.metadataObjectTypes = [.qr]
            }

            let previewLayer = AVCaptureVideoPreviewLayer(session: session)
            previewLayer.videoGravity = .resizeAspectFill
            previewLayer.frame = bounds
            layer.addSublayer(previewLayer)
            self.previewLayer = previewLayer

            DispatchQueue.global(qos: .userInitiated).async {
                session.startRunning()
            }
        } catch {
            print("Failed to setup camera: \(error)")
        }
    }

    func setTorch(on: Bool) {
        guard let device = AVCaptureDevice.default(for: .video),
              device.hasTorch else { return }

        do {
            try device.lockForConfiguration()
            device.torchMode = on ? .on : .off
            device.unlockForConfiguration()
        } catch {
            print("Failed to toggle torch: \(error)")
        }
    }

    deinit {
        captureSession?.stopRunning()
    }
}

extension QRCameraUIView: AVCaptureMetadataOutputObjectsDelegate {
    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        guard let metadataObject = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              metadataObject.type == .qr,
              let code = metadataObject.stringValue else { return }

        // Debounce: don't process the same code repeatedly
        let now = Date()
        if code == lastScannedCode,
           let lastTime = lastScanTime,
           now.timeIntervalSince(lastTime) < 2.0 {
            return
        }

        lastScannedCode = code
        lastScanTime = now

        // Haptic feedback
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)

        onCodeScanned?(code)
    }
}

/// Visual overlay for the scanner
struct ScannerOverlay: View {
    let colors: CladColors.ColorScheme
    let frameSize: CGFloat = 280

    var body: some View {
        ZStack {
            // Semi-transparent background
            Color.black.opacity(0.5)

            // Cut out the scanning area
            Rectangle()
                .frame(width: frameSize, height: frameSize)
                .blendMode(.destinationOut)

            // Corner brackets
            ScannerCorners(size: frameSize, color: colors.primary)
        }
        .compositingGroup()
    }
}

/// Corner bracket decorations for the scanner frame
struct ScannerCorners: View {
    let size: CGFloat
    let color: Color
    let cornerLength: CGFloat = 30
    let lineWidth: CGFloat = 4

    var body: some View {
        ZStack {
            // Top-left
            CornerBracket(rotation: 0)
                .offset(x: -size/2 + cornerLength/2, y: -size/2 + cornerLength/2)

            // Top-right
            CornerBracket(rotation: 90)
                .offset(x: size/2 - cornerLength/2, y: -size/2 + cornerLength/2)

            // Bottom-right
            CornerBracket(rotation: 180)
                .offset(x: size/2 - cornerLength/2, y: size/2 - cornerLength/2)

            // Bottom-left
            CornerBracket(rotation: 270)
                .offset(x: -size/2 + cornerLength/2, y: size/2 - cornerLength/2)
        }
    }

    @ViewBuilder
    func CornerBracket(rotation: Double) -> some View {
        Path { path in
            path.move(to: CGPoint(x: 0, y: cornerLength))
            path.addLine(to: CGPoint(x: 0, y: 0))
            path.addLine(to: CGPoint(x: cornerLength, y: 0))
        }
        .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round, lineJoin: .round))
        .frame(width: cornerLength, height: cornerLength)
        .rotationEffect(.degrees(rotation))
    }
}

/// View shown when camera permission is denied
struct CameraPermissionDeniedView: View {
    let colors: CladColors.ColorScheme

    var body: some View {
        ZStack {
            colors.background
                .ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "camera.fill")
                    .font(.system(size: 60))
                    .foregroundColor(colors.onSurfaceVariant)

                Text("Camera Access Required")
                    .font(CladTypography.headlineMedium)
                    .foregroundColor(colors.onBackground)

                Text("Please enable camera access in Settings to scan QR codes.")
                    .font(CladTypography.bodyMedium)
                    .foregroundColor(colors.onSurfaceVariant)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)

                Button(action: openSettings) {
                    Text("Open Settings")
                        .font(CladTypography.bodyLarge)
                        .fontWeight(.semibold)
                        .foregroundColor(colors.onPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(16)
                        .background(colors.primary)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 40)
            }
        }
    }

    private func openSettings() {
        if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(settingsURL)
        }
    }
}
