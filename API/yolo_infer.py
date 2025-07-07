import numpy as np
from ultralytics import YOLO
import cv2

def load_yolo_model(weight_path: str):
    return YOLO(weight_path)

def run_yolo(model, image_np: np.ndarray) -> np.ndarray:
    results = model.predict(image_np, conf=0.4)[0]

    fat_class_id = [k for k, v in results.names.items() if v == 'fat']
    if not fat_class_id:
        return image_np
    fat_class_id = fat_class_id[0]

    fat_indices = (results.boxes.cls == fat_class_id).nonzero().flatten()
    if len(fat_indices) == 0:
        return image_np

    if hasattr(results, "masks") and results.masks is not None:
        masks = results.masks.data[fat_indices].cpu().numpy()
        mask_overlay = image_np.copy()

        h, w = image_np.shape[:2]
        for mask in masks:
            resized_mask = cv2.resize(mask.astype(np.float32), (w, h), interpolation=cv2.INTER_NEAREST)
            binary_mask = resized_mask > 0.5

            colored_mask = np.zeros_like(image_np, dtype=np.uint8)
            colored_mask[binary_mask] = (144, 53, 223)  

            alpha = 0.4
            mask_overlay = cv2.addWeighted(mask_overlay, 1.0, colored_mask, alpha, 0)

        return mask_overlay
    else:
        return image_np
