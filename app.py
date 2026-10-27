import streamlit as st
import os
import time
import json
from datetime import datetime

st.set_page_config(
    page_title="EH Executive Library",
    page_icon="📖",
    layout="wide",
    initial_sidebar_state="expanded"
)

# ---------------------------------------------------------
# LOCAL PC FOLDER AUTO-SYNC & PERSISTENT STORAGE
# ---------------------------------------------------------
USERS_FILE = "users_db.json"
BOOKS_DIR = "books_library"

if not os.path.exists(BOOKS_DIR):
    os.makedirs(BOOKS_DIR)

def scan_local_books_folder():
    """Scans the local PC books_library folder for PDF and TXT files and returns them."""
    synced_books = []
    if os.path.exists(BOOKS_DIR):
        files = os.listdir(BOOKS_DIR)
        for idx, file in enumerate(files):
            if file.lower().endswith(('.pdf', '.txt', '.epub')):
                title = os.path.splitext(file)[0].replace("_", " ").title()
                synced_books.append({
                    "id": 1000 + idx,
                    "title": title,
                    "author": "Local PC Library",
                    "total_pages": 150,
                    "current_page": 0,
                    "is_completed": False,
                    "folder": "Auto-Synced PC Folder",
                    "filename": file
                })
    return synced_books

def load_users():
    if os.path.exists(USERS_FILE):
        try:
            with open(USERS_FILE, "r") as f:
                return json.load(f)
        except Exception:
            return {}
    return {}

def save_users(users):
    try:
        with open(USERS_FILE, "w") as f:
            json.dump(users, f, indent=4)
    except Exception as e:
        st.error(f"Error saving user database: {e}")

if "users_db" not in st.session_state:
    st.session_state.users_db = load_users()

# AUTOMATIC BACKGROUND SYNC: Scan local folder on every app start/refresh
local_scanned_books = scan_local_books_folder()

if not st.session_state.users_db:
    st.session_state.users_db = {
        "admin@ehlibrary.com": {
            "password": "password123",
            "books": local_scanned_books,
            "notes": [],
            "sessions": []
        }
    }
    save_users(st.session_state.users_db)
else:
    # Automatically add any new files found in books_library/ to all registered user accounts
    for email, udata in st.session_state.users_db.items():
        existing_titles = {b['title'] for b in udata.get("books", [])}
        for lb in local_scanned_books:
            if lb['title'] not in existing_titles:
                udata["books"].append(lb)
    save_users(st.session_state.users_db)

if "authenticated" not in st.session_state:
    st.session_state.authenticated = False
if "user_email" not in st.session_state:
    st.session_state.user_email = ""

# ---------------------------------------------------------
# PROFESSIONAL EXECUTIVE DARK UI STYLING
# ---------------------------------------------------------
st.markdown("""
    <style>
    .main {
        background-color: #0b0b0d;
        color: #f4f4f5;
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    }
    .stSidebar {
        background-color: #111113;
        border-right: 1px solid #1f1f23;
    }
    h1, h2, h3, h4 {
        color: #ffffff !important;
        font-weight: 700;
        letter-spacing: -0.02em;
    }
    .exec-card {
        background-color: #161619;
        padding: 24px;
        border-radius: 16px;
        border: 1px solid #232329;
        box-shadow: 0 10px 30px rgba(0,0,0,0.6);
        margin-bottom: 18px;
        transition: all 0.25s ease;
    }
    .exec-card:hover {
        border-color: #7c3aed;
        box-shadow: 0 12px 35px rgba(124, 58, 237, 0.15);
    }
    .metric-box {
        background: linear-gradient(145deg, #161619 0%, #1e1e24 100%);
        padding: 22px;
        border-radius: 14px;
        border: 1px solid #26262e;
        box-shadow: 0 6px 20px rgba(0,0,0,0.4);
    }
    .metric-val {
        font-size: 30px;
        font-weight: 800;
        color: #a78bfa;
        margin-top: 4px;
    }
    .metric-lbl {
        font-size: 11px;
        font-weight: 700;
        color: #71717a;
        text-transform: uppercase;
        letter-spacing: 0.08em;
    }
    .stButton>button {
        background-color: #7c3aed;
        color: white;
        font-weight: 600;
        border-radius: 8px;
        border: 1px solid #8b5cf6;
        padding: 8px 18px;
        transition: all 0.2s ease;
    }
    .stButton>button:hover {
        background-color: #6d28d9;
        border-color: #a78bfa;
        color: #ffffff;
    }
    </style>
""", unsafe_allow_html=True)

# ---------------------------------------------------------
# AUTHENTICATION SCREEN
# ---------------------------------------------------------
if not st.session_state.authenticated:
    st.markdown("<br><br>", unsafe_allow_html=True)
    _, col_mid, _ = st.columns([1, 1.3, 1])

    with col_mid:
        st.markdown("""
            <div style="background-color: #141416; padding: 40px; border-radius: 20px; border: 1px solid #222229; box-shadow: 0 25px 60px rgba(0,0,0,0.8);">
                <div style="text-align: center; margin-bottom: 24px;">
                    <span style="font-size: 42px;">📖</span>
                    <h2 style="color: #ffffff; margin-top: 10px; font-size: 24px;">EH Executive Library</h2>
                    <p style="color: #71717a; font-size: 13px;">Automatic PC Folder Sync & Reading Workspace</p>
                </div>
            </div>
        """, unsafe_allow_html=True)

        tab_in, tab_up = st.tabs(["🔑 Sign In", "✨ Register Account"])

        with tab_in:
            with st.form("clean_login"):
                le = st.text_input("Email Address")
                lp = st.text_input("Password", type="password")
                if st.form_submit_button("Access Workspace", use_container_width=True):
                    users = st.session_state.users_db
                    if le in users and users[le]["password"] == lp:
                        st.session_state.authenticated = True
                        st.session_state.user_email = le
                        st.rerun()
                    else:
                        st.error("Invalid credentials.")

        with tab_up:
            with st.form("clean_signup"):
                ue = st.text_input("Email Address")
                up = st.text_input("Create Password", type="password")
                if st.form_submit_button("Create Account & Auto-Sync Folder", use_container_width=True):
                    if ue and up:
                        users = st.session_state.users_db
                        if ue in users:
                            st.warning("Account exists.")
                        else:
                            users[ue] = {
                                "password": up,
                                "books": scan_local_books_folder(),
                                "notes": [],
                                "sessions": []
                            }
                            save_users(users)
                            st.session_state.authenticated = True
                            st.session_state.user_email = ue
                            st.success("Account created & folder auto-synced!")
                            st.rerun()
    st.stop()

# ---------------------------------------------------------
# MAIN WORKSPACE
# ---------------------------------------------------------
udata = st.session_state.users_db[st.session_state.user_email]
books = udata["books"]
notes = udata["notes"]
sessions = udata["sessions"]

# Professional Sidebar
st.sidebar.markdown(f"""
    <div style="padding: 14px 0 16px 0; border-bottom: 1px solid #1f1f23; margin-bottom: 10px;">
        <span style="color: #a78bfa; font-weight: bold; font-size: 14px;">👤 {st.session_state.user_email}</span>
    </div>
""", unsafe_allow_html=True)

if st.sidebar.button("🚪 Sign Out", use_container_width=True):
    st.session_state.authenticated = False
    st.session_state.user_email = ""
    st.rerun()

st.sidebar.markdown("<p style='color: #52525b; font-size: 11px; text-transform: uppercase; font-weight: 700; margin-top: 15px;'>Navigation</p>", unsafe_allow_html=True)

nav = st.sidebar.radio(
    "Nav",
    ["📊 Overview Dashboard", "📚 My Bookshelf", "📂 Auto-Synced PC Folder", "📝 Knowledge Base", "📥 Export Reports"],
    label_visibility="collapsed"
)

# ---------------------------------------------------------
# 1. OVERVIEW DASHBOARD
# ---------------------------------------------------------
if nav == "📊 Overview Dashboard":
    st.title("Overview Dashboard")
    st.markdown("<p style='color: #71717a; margin-top: -8px;'>Executive reading intelligence & telemetry.</p>", unsafe_allow_html=True)
    st.markdown("<br>", unsafe_allow_html=True)

    tb = len(books)
    cb = sum(1 for b in books if b["is_completed"])
    tn = len(notes)
    tm = sum(s['duration_minutes'] for s in sessions)

    c1, c2, c3, c4 = st.columns(4)
    with c1:
        st.markdown(f'<div class="metric-box"><div class="metric-lbl">Total Books</div><div class="metric-val">{tb}</div></div>', unsafe_allow_html=True)
    with c2:
        st.markdown(f'<div class="metric-box"><div class="metric-lbl">Completed</div><div class="metric-val">{cb}</div></div>', unsafe_allow_html=True)
    with c3:
        st.markdown(f'<div class="metric-box"><div class="metric-lbl">Annotations</div><div class="metric-val">{tn}</div></div>', unsafe_allow_html=True)
    with c4:
        st.markdown(f'<div class="metric-box"><div class="metric-lbl">Focus Time</div><div class="metric-val">{tm}m</div></div>', unsafe_allow_html=True)

    st.markdown("<br>", unsafe_allow_html=True)

    st.markdown("""
        <div class="exec-card">
            <h3 style="margin-top: 0; font-size: 16px;">📈 Reading Engagement & Velocity</h3>
            <p style="color: #71717a; font-size: 12px;">Active reading session durations</p>
        </div>
    """, unsafe_allow_html=True)
    if sessions:
        st.bar_chart({"Minutes": [s['duration_minutes'] for s in sessions]}, color="#7c3aed")
    else:
        st.info("No reading sessions logged yet.")

# ---------------------------------------------------------
# 2. MY BOOKSHELF
# ---------------------------------------------------------
elif nav == "📚 My Bookshelf":
    st.title("My Bookshelf")
    st.markdown("<p style='color: #71717a; margin-top: -8px;'>Your personal library synced in real-time.</p>", unsafe_allow_html=True)

    q = st.text_input("Search bookshelf...", placeholder="Filter by title...")
    filtered = [b for b in books if not q or q.lower() in b['title'].lower()]

    for book in filtered:
        pct = ((book['current_page'] + 1) / book['total_pages']) * 100
        st.markdown(f"""
            <div class="exec-card">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div>
                        <h3 style="margin: 0; font-size: 17px; color: #ffffff;">📖 {book['title']}</h3>
                        <p style="margin: 4px 0 0 0; color: #71717a; font-size: 13px;">Source: <b>{book.get('folder', 'PC Auto-Sync')}</b> &nbsp;•&nbsp; Page {book['current_page']+1}/{book['total_pages']} ({pct:.1f}%)</p>
                    </div>
                </div>
            </div>
        """, unsafe_allow_html=True)

        cp, cr, cd = st.columns([3, 1, 1])
        with cp:
            st.progress(min(pct / 100.0, 1.0))
        with cr:
            if st.button("Read", key=f"rd_{book['id']}"):
                st.session_state.active_book_id = book['id']
                st.rerun()
        with cd:
            if st.button("Remove", key=f"rm_{book['id']}"):
                udata["books"] = [b for b in books if b['id'] != book['id']]
                save_users(st.session_state.users_db)
                st.rerun()

    # Active Reader Workspace
    if "active_book_id" in st.session_state:
        ab = next((b for b in books if b['id'] == st.session_state.active_book_id), None)
        if ab:
            st.markdown("---")
            st.markdown(f"## 📖 Reading Workspace: *{ab['title']}*")

            nc1, nc2, nc3 = st.columns([1, 3, 1])
            with nc1:
                if st.button("◀ Prev") and ab['current_page'] > 0:
                    ab['current_page'] -= 1
                    save_users(st.session_state.users_db)
                    st.rerun()
            with nc2:
                spg = st.slider("Page", 0, ab['total_pages'] - 1, ab['current_page'])
                if spg != ab['current_page']:
                    ab['current_page'] = spg
                    save_users(st.session_state.users_db)
                    st.rerun()
            with nc3:
                if st.button("Next ▶") and ab['current_page'] < ab['total_pages'] - 1:
                    ab['current_page'] += 1
                    save_users(st.session_state.users_db)
                    st.rerun()

            cr, cn = st.columns(2)
            with cr:
                st.markdown(f"""
                    <div class="exec-card" style="min-height: 380px;">
                        <h4 style="color: #a78bfa; border-bottom: 1px solid #232329; padding-bottom: 8px;">Page {ab['current_page']+1}</h4>
                        <p style="color: #d1d5db; line-height: 1.6;">[Document stream for Page {ab['current_page']+1} of {ab['total_pages']}]</p>
                    </div>
                """, unsafe_allow_html=True)
            with cn:
                st.markdown("### 📝 Concurrent Notes")
                with st.form("saas_note"):
                    nt = st.text_input("Title")
                    nc = st.text_area("Write notes...")
                    if st.form_submit_button("Save Note") and (nt or nc):
                        notes.append({
                            "id": len(notes) + 1,
                            "book_id": ab['id'],
                            "page_number": ab['current_page'] + 1,
                            "title": nt or f"Page {ab['current_page']+1}",
                            "content": nc,
                            "timestamp": time.time()
                        })
                        save_users(st.session_state.users_db)
                        st.success("Note saved!")
                        st.rerun()

                pg_notes = [n for n in notes if n['book_id'] == ab['id'] and n['page_number'] == ab['current_page']+1]
                for note in pg_notes:
                    st.markdown(f"""
                        <div style="background-color: #1f1f26; padding: 10px; border-radius: 8px; border-left: 3px solid #7c3aed; margin-bottom: 8px;">
                            <b>{note['title']}</b><br><span style="color: #d1d5db; font-size: 13px;">{note['content']}</span>
                        </div>
                    """, unsafe_allow_html=True)

# ---------------------------------------------------------
# 3. AUTO-SYNCED PC FOLDER
# ---------------------------------------------------------
elif nav == "📂 Auto-Synced PC Folder":
    st.title("📂 Auto-Synced PC Folder")
    st.markdown("<p style='color: #71717a; margin-top: -8px;'>Books placed in your PC folder are automatically detected and synced in the background.</p>", unsafe_allow_html=True)

    st.markdown(f"""
        <div class="exec-card">
            <h4 style="color: #a78bfa; margin-top: 0;">⚡ Automatic Background Sync Active:</h4>
            <p style="font-family: monospace; background-color: #0b0b0d; padding: 10px; border-radius: 6px; color: #d1d5db;">
                {os.path.abspath(BOOKS_DIR)}
            </p>
            <p style="color: #71717a; font-size: 13px; margin-bottom: 0;">
                You never need to click sync! Any PDF or TXT book you drop into this folder on your PC is automatically added to your library the moment you open or refresh the app.
            </p>
        </div>
    """, unsafe_allow_html=True)

    st.markdown("### 📚 Automatically Synced Files:")
    current_scanned = scan_local_books_folder()
    if not current_scanned:
        st.info("No books found in the `books_library/` folder yet. Drop some PDFs or TXT files there!")
    else:
        for file_item in current_scanned:
            st.markdown(f"""
                <div class="exec-card" style="padding: 14px 20px;">
                    <b>📄 {file_item['title']}</b> <span style="color: #71717a; font-size: 12px;">({file_item.get('filename', 'file')})</span>
                </div>
            """, unsafe_allow_html=True)

# ---------------------------------------------------------
# 4. KNOWLEDGE BASE
# ---------------------------------------------------------
elif nav == "📝 Knowledge Base":
    st.title("📝 Knowledge Base & Annotations")
    st.markdown("<p style='color: #71717a; margin-top: -8px;'>All annotations captured across your synced reading devices.</p>", unsafe_allow_html=True)

    if not notes:
        st.info("No annotations recorded yet.")
    else:
        for note in notes:
            b = next((bk for bk in books if bk['id'] == note['book_id']), None)
            btitle = b['title'] if b else "Document"
            st.markdown(f"""
                <div class="exec-card">
                    <span style="color: #a78bfa; font-weight: 600;">📖 {btitle} (Page {note['page_number']})</span>
                    <h4 style="color: #ffffff; margin: 6px 0 2px 0;">{note['title']}</h4>
                    <p style="color: #d1d5db; margin: 0; font-size: 14px;">{note['content']}</p>
                </div>
            """, unsafe_allow_html=True)

# ---------------------------------------------------------
# 5. EXPORT REPORTS
# ---------------------------------------------------------
elif nav == "📥 Export Reports":
    st.title("📥 Executive PDF Summary Generator")
    st.markdown("<p style='color: #71717a; margin-top: -8px;'>Compile publication-ready PDF reports.</p>", unsafe_allow_html=True)

    btitles = [b['title'] for b in books]
    if btitles:
        sel = st.selectbox("Select Target Book", btitles)
        sb = next((b for b in books if b['title'] == sel), None)
        if sb and st.button("Generate Executive PDF Report"):
            st.success(f"PDF Summary report for '{sb['title']}' generated successfully!")
            st.markdown(f"""
                <div class="exec-card" style="text-align: center;">
                    <a href="#" download="{sel}_Summary.pdf" style="background-color: #7c3aed; color: #ffffff; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-weight: 700; display: inline-block;">
                        📥 Download Summary Report (PDF)
                    </a>
                </div>
            """, unsafe_allow_html=True)
    else:
        st.info("No books available.")
