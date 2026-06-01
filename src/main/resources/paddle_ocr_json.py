import argparse
import json

from paddleocr import PaddleOCR


def normalize_result(raw):
    blocks = []
    for item in flatten(raw):
        parsed = parse_item(item)
        if isinstance(parsed, list):
            blocks.extend(parsed)
        elif parsed:
            blocks.append(parsed)
    return {
        "text": "\n".join(block["text"] for block in blocks),
        "blocks": blocks,
    }


def flatten(value):
    if value is None:
        return
    if isinstance(value, dict):
        yield value
        return
    if not isinstance(value, list):
        return
    if is_ocr_line(value):
        yield value
        return
    for item in value:
        yield from flatten(item)


def is_ocr_line(value):
    return (
        isinstance(value, list)
        and len(value) >= 2
        and isinstance(value[0], list)
        and isinstance(value[1], (list, tuple))
        and len(value[1]) >= 2
    )


def parse_item(item):
    if isinstance(item, dict):
        text = item.get("text") or item.get("rec_text")
        confidence = item.get("confidence") or item.get("score") or item.get("rec_score") or 0
        box = item.get("box") or item.get("dt_polys") or item.get("bbox") or []
        if text:
            return {"text": str(text), "confidence": float(confidence), "box": box}
        texts = item.get("rec_texts") or item.get("texts")
        scores = item.get("rec_scores") or item.get("scores") or []
        boxes = item.get("dt_polys") or item.get("rec_polys") or item.get("boxes") or []
        if isinstance(texts, list):
            parsed = []
            for index, value in enumerate(texts):
                if value is None or str(value).strip() == "":
                    continue
                parsed.append({
                    "text": str(value),
                    "confidence": float(scores[index]) if index < len(scores) else 0,
                    "box": boxes[index] if index < len(boxes) else [],
                })
            return parsed
        return None
    if is_ocr_line(item):
        text, confidence = item[1][0], item[1][1]
        return {"text": str(text), "confidence": float(confidence), "box": item[0]}
    return None


def parse_bool(value):
    return str(value).lower() in {"1", "true", "yes", "y", "on"}


def parse_args():
    parser = argparse.ArgumentParser(description="Run PaddleOCR and print normalized JSON output.")
    parser.add_argument("image_path", nargs="?", help="Image path kept for backward compatibility.")
    parser.add_argument("--image", dest="image", help="Image path to recognize.")
    parser.add_argument("--lang", default="ch", help="PaddleOCR language, for example ch or en.")
    parser.add_argument("--use-angle-cls", default="true", help="Whether to enable PaddleOCR angle classification.")
    args = parser.parse_args()
    image_path = args.image or args.image_path
    if not image_path:
        parser.error("image path is required")
    return image_path, args.lang, parse_bool(args.use_angle_cls)


def main():
    image_path, language, use_angle_cls = parse_args()
    ocr = PaddleOCR(use_angle_cls=use_angle_cls, lang=language)
    if hasattr(ocr, "ocr"):
        raw = ocr.ocr(image_path, cls=use_angle_cls)
    else:
        raw = ocr.predict(image_path)
    print(json.dumps(normalize_result(raw), ensure_ascii=False))


if __name__ == "__main__":
    main()
