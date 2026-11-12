# ChessQ Lite

`chessq-lite-v5-int8.onnx` is the [ChessQ Lite V5 INT8 release](https://huggingface.co/joelseytre/chessq-lite/blob/fa83b74862c96c137f59ab4d11ed9431a8efbad5/cq-vit-s-644-v5-seed1-onnx-int8-r1.onnx), renamed for this directory.

- Size: 35,905,331 bytes.
- SHA-256: `334c8b19b4f3fd92e95f74ec75e0e34e86e5c95bcd7c090c972d126497bd5da3`.
- Input: float32 RGB `image[1,3,644,644]`, with ImageNet normalization.
- Outputs: float32 `logits[1,64,13]` in FEN order and `direction[1,2]` as `[cos, sin]`.
- Direction points from e1 toward e8, with cosine upward and sine rightward. Viewing angles within ±100°, including endpoints, show White at the bottom; other angles show Black. Orientation does not change the FEN or side to move.

The [model card](https://huggingface.co/joelseytre/chessq-lite/blob/6ce1f1aebde605347056586a183356a4ccb2eaaa/README.md) includes release manifests, validation and limitations. The editor uses ONNX Runtime Web 1.30.0 with its existing canvas preprocessing.

`chessq-lite-v4-int8.onnx` is the previous quantized model, retained for existing clients. Its [source release](https://huggingface.co/joelseytre/chessq-lite/blob/main/cq-vit-s-644-v4-seed2-safetensors-fp16-r2.json) is unchanged.

The ViT-S weights remain under [PolyForm Noncommercial License 1.0.0](https://huggingface.co/joelseytre/chessq-lite/blob/6ce1f1aebde605347056586a183356a4ccb2eaaa/LICENSE).
