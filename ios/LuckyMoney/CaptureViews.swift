import PhotosUI
import SwiftUI
import UIKit

struct CaptureFlowView: View {
    let categories: [LMCategory]
    let onDraft: (LMExpenseDraft) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedItem: PhotosPickerItem?
    @State private var showingCamera = false
    @State private var isAnalyzing = false
    @State private var errorMessage = ""

    var body: some View {
        NavigationStack {
            VStack(spacing: 18) {
                Image(systemName: "camera.viewfinder")
                    .font(.system(size: 68, weight: .bold))
                    .foregroundStyle(.green)
                Text("capture").font(.title.bold())
                Text("Take a receipt/object photo or import from Photos. You always confirm before saving.")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)

                Button {
                    showingCamera = true
                } label: {
                    Label("Camera", systemImage: "camera.fill").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                PhotosPicker(selection: $selectedItem, matching: .images) {
                    Label("Photo library", systemImage: "photo.on.rectangle").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                if isAnalyzing {
                    ProgressView("Analyzing")
                }
                if !errorMessage.isEmpty {
                    Text(errorMessage).font(.caption).foregroundStyle(.red)
                }
                Spacer()
            }
            .padding(24)
            .navigationTitle("capture")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("cancel") { dismiss() }
                }
            }
        }
        .sheet(isPresented: $showingCamera) {
            CameraPicker { image in
                Task { await analyze(image: image, localPath: nil) }
            }
        }
        .onChange(of: selectedItem) { _, item in
            guard let item else { return }
            Task {
                do {
                    guard let data = try await item.loadTransferable(type: Data.self), let image = UIImage(data: data) else {
                        errorMessage = "Could not load image"
                        return
                    }
                    await analyze(image: image, localPath: nil)
                } catch {
                    errorMessage = error.localizedDescription
                }
            }
        }
    }

    @MainActor
    private func analyze(image: UIImage, localPath: String?) async {
        isAnalyzing = true
        errorMessage = ""
        let suggestion = await LMExpenseAnalyzer.analyze(image: image, categories: categories)
        isAnalyzing = false
        onDraft(LMExpenseDraft(suggestion: suggestion, receiptPath: localPath))
    }
}

struct CameraPicker: UIViewControllerRepresentable {
    let onImage: (UIImage) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onImage: onImage)
    }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = UIImagePickerController.isSourceTypeAvailable(.camera) ? .camera : .photoLibrary
        picker.allowsEditing = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    final class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let onImage: (UIImage) -> Void

        init(onImage: @escaping (UIImage) -> Void) {
            self.onImage = onImage
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage {
                onImage(image)
            }
            picker.dismiss(animated: true)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            picker.dismiss(animated: true)
        }
    }
}
