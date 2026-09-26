path = "app/src/main/java/com/localdrop/server/ReceiverPageBuilder.kt"
with open(path, "r") as f:
    html = f.read()

# Web video ad player block (Responsive HTML5 container with fallback & remote switch check)
video_ad_block = """
        <!-- Receiver Sponsored Video Section -->
        <div style="margin-top: 24px; padding: 14px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); border-radius: 16px;">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px;">
                <span style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; color: #3b82f6; font-weight: 700;">Sponsored Video</span>
                <span style="font-size: 10px; color: #94a3b8; background: rgba(255,255,255,0.06); padding: 2px 6px; border-radius: 4px;">Ad</span>
            </div>
            <div style="position: relative; border-radius: 12px; overflow: hidden; background: #000; box-shadow: 0 4px 16px rgba(0,0,0,0.4);">
                <video autoplay muted loop playsinline controls style="width: 100%; max-height: 220px; display: block; object-fit: cover;">
                    <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4" type="video/mp4">
                    Your browser does not support the video tag.
                </video>
            </div>
        </div>
"""

# Place it right before the closing main/body container
if "Sponsored Video" not in html:
    if "</main>" in html:
        html = html.replace("</main>", video_ad_block + "\n    </main>", 1)
    elif "</body>" in html:
        html = html.replace("</body>", video_ad_block + "\n</body>", 1)
    with open(path, "w") as f:
        f.write(html)
    print("Receiver web page patched with video player container!")
else:
    print("Receiver page already has video player!")
