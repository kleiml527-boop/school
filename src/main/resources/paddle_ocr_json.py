import json
import sys

from paddleocr import PaddleOCR


def normalize_result(raw):
    blocks = []
    for item in flatten(raw):
        parsed = parse_item(item)
        if parsed:
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
        return None
    if is_ocr_line(item):
        text, confidence = item[1][0], item[1][1]
        return {"text": str(text), "confidence": float(confidence), "box": item[0]}
    return None


def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: paddle_ocr_json.py <image-path>")
    ocr = PaddleOCR(use_angle_cls=True, lang="ch")
    if hasattr(ocr, "ocr"):
        raw = ocr.ocr(sys.argv[1], cls=True)
    else:
        raw = ocr.predict(sys.argv[1])
    print(json.dumps(normalize_result(raw), ensure_ascii=False))


if __name__ == "__main__":
    main()
