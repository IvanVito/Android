from flask import Flask, request
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 100 * 1024 * 1024  # 100 MB
UPLOAD_FOLDER = "uploads"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route("/upload", methods=["POST"])
def upload_files():
    if 'files' not in request.files:
        return "Нет файлов для загрузки", 400

    files = request.files.getlist("files")
    saved = []
    for file in files:
        filename = secure_filename(file.filename)
        if not filename:
            continue
        save_path = os.path.join(UPLOAD_FOLDER, filename)
        file.save(save_path)   # FileStorage.save использует потоковое сохранение
        saved.append(filename)

    return "Saved: " + ", ".join(saved), 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
