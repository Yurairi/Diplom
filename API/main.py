from fastapi import FastAPI, File, UploadFile
from fastapi.responses import StreamingResponse
import io
from PIL import Image
import numpy as np
import cv2
from models.yolo_infer import load_yolo_model, run_yolo

app = FastAPI()

yolo_mobile = load_yolo_model('weights/best_mobile.pt')

@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    image_bytes = await file.read()

    model = yolo_mobile

    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    image_np = np.array(image)

    result_img = run_yolo(model, image_np)

    result_img_bgr = cv2.cvtColor(result_img, cv2.COLOR_RGB2BGR)
    _, img_encoded = cv2.imencode('.jpg', result_img_bgr)

    return StreamingResponse(io.BytesIO(img_encoded.tobytes()), media_type="image/jpeg")

