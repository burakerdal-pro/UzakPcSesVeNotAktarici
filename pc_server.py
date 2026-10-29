import base64
import json
import os
import socket
import subprocess
from http.server import BaseHTTPRequestHandler, HTTPServer


def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def get_save_directory():
    user_home = os.path.expanduser("~")
    desktop_en = os.path.join(user_home, "Desktop")
    desktop_tr = os.path.join(user_home, "Masaüstü")

    if os.path.exists(desktop_en):
        return desktop_en
    elif os.path.exists(desktop_tr):
        return desktop_tr
    else:
        return os.getcwd()


class NoteRequestHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path == "/ping":
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.end_headers()
            response = {"status": "ok", "message": "PC Sunucusu Aktif!"}
            self.wfile.write(json.dumps(response, ensure_ascii=False).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if self.path == "/note":
            content_length = int(self.headers.get("Content-Length", 0))
            post_data = self.rfile.read(content_length)

            try:
                data = json.loads(post_data.decode("utf-8"))
                filename = data.get("filename", "notlar.txt").strip()
                content = data.get("content", "")
                append = data.get("append", True)
                open_notepad = data.get("open_notepad", True)

                safe_filename = os.path.basename(filename)
                if not safe_filename:
                    safe_filename = "notlar.txt"

                save_dir = get_save_directory()
                file_path = os.path.join(save_dir, safe_filename)
                mode = "a" if append else "w"

                file_exists = os.path.exists(file_path)
                with open(file_path, mode, encoding="utf-8") as f:
                    if append and file_exists and os.path.getsize(file_path) > 0:
                        f.write("\n" + content)
                    else:
                        f.write(content)

                print(
                    f"[+] Not kaydedildi: {file_path} (Mod: {'Ekle' if append else 'Üstüne Yaz'})"
                )

                if open_notepad:
                    try:
                        if os.name == "nt":
                            subprocess.Popen(["notepad.exe", file_path])
                        else:
                            print(f"[i] Dosya kaydedildi: {file_path}")
                    except Exception as err:
                        print(f"[!] Notepad açılırken hata: {err}")

                self.send_response(200)
                self.send_header(
                    "Content-Type", "application/json; charset=utf-8"
                )
                self.end_headers()
                response = {
                    "status": "success",
                    "message": f"Not Masaüstü'ndeki '{safe_filename}' dosyasına kaydedildi!",
                }
                self.wfile.write(json.dumps(response, ensure_ascii=False).encode("utf-8"))

            except Exception as e:
                print(f"[!] Hata oluştu: {e}")
                self.send_response(500)
                self.send_header(
                    "Content-Type", "application/json; charset=utf-8"
                )
                self.end_headers()
                response = {"status": "error", "message": str(e)}
                self.wfile.write(json.dumps(response, ensure_ascii=False).encode("utf-8"))

        elif self.path == "/audio":
            content_length = int(self.headers.get("Content-Length", 0))
            post_data = self.rfile.read(content_length)

            try:
                data = json.loads(post_data.decode("utf-8"))
                filename = data.get("filename", "ses_kaydi.m4a").strip()
                audio_b64 = data.get("audio_data", "")

                safe_filename = os.path.basename(filename)
                if not safe_filename or not safe_filename.endswith((".m4a", ".wav", ".mp3", ".3gp")):
                    safe_filename = "ses_kaydi.m4a"

                save_dir = get_save_directory()
                file_path = os.path.join(save_dir, safe_filename)

                audio_bytes = base64.b64decode(audio_b64)
                with open(file_path, "wb") as f:
                    f.write(audio_bytes)

                print(f"[+] Ses kaydı kaydedildi: {file_path}")

                try:
                    if os.name == "nt":
                        os.startfile(file_path)
                except Exception as err:
                    print(f"[!] Ses dosyası açılırken hata: {err}")

                self.send_response(200)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.end_headers()
                response = {
                    "status": "success",
                    "message": f"Ses kaydı Masaüstü'ne '{safe_filename}' olarak kaydedildi ve açıldı!",
                }
                self.wfile.write(json.dumps(response, ensure_ascii=False).encode("utf-8"))

            except Exception as e:
                print(f"[!] Ses kaydı işlenirken hata: {e}")
                self.send_response(500)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.end_headers()
                response = {"status": "error", "message": str(e)}
                self.wfile.write(json.dumps(response, ensure_ascii=False).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()


def run(port=5000):
    local_ip = get_local_ip()
    save_dir = get_save_directory()
    server_address = ("0.0.0.0", port)
    httpd = HTTPServer(server_address, NoteRequestHandler)
    print("=" * 60)
    print("      PC NOT DEFTERİ & SES SUNUCUSU BAŞLATILDI")
    print("=" * 60)
    print(f"Bilgisayarınızın Yerel IP Adresi : {local_ip}")
    print(f"Dinlenen Port                     : {port}")
    print(f"Dosyaların Kaydedileceği Klasör  : {save_dir}")
    print(
        f"\nTelefondaki uygulamaya IP adresi olarak '{local_ip}' girin."
    )
    print("Sunucu çalışıyor... Çıkmak için Ctrl+C tuşlarına basın.\n")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nSunucu kapatıldı.")


if __name__ == "__main__":
    run(port=5000)
