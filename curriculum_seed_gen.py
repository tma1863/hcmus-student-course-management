#!/usr/bin/env python3
"""
Generate curriculum seed SQL for the 3 existing major-programs.

Rules enforced:
- ~50-60 courses per program, 8 semesters.
- Required course  -> 3 or 4 credits ; Optional course -> 1 or 2 credits.
- A course may belong to many programs (shared blocks below); program_semester /
  prerequisites can differ per program (course_majors row is per-program).
- Prerequisites reference foundational Courses and must sit in a STRICTLY EARLIER
  program_semester within the same program  =>  guarantees an acyclic graph.
- course_id business key: 3-letter prefix + 5-digit sequence, incremented per prefix.
"""

# ---------------------------------------------------------------------------
# Programs that already exist in the DB (major_programs.id)
# 1 = Khoa hoc Du lieu (Data Science), 2 = He thong Thong tin (Information Systems),
# 3 = Tri tue Nhan tao (Artificial Intelligence)
# ---------------------------------------------------------------------------

# Catalog: course_key -> (prefix, Vietnamese name)
CATALOG = {
    # --- General education (shared by all programs) ---
    "phil1": ("PHL", "Triết học Mác - Lênin"),
    "phil2": ("PHL", "Kinh tế chính trị Mác - Lênin"),
    "phil3": ("PHL", "Chủ nghĩa xã hội khoa học"),
    "hist1": ("HIS", "Lịch sử Đảng Cộng sản Việt Nam"),
    "hcm1":  ("HCM", "Tư tưởng Hồ Chí Minh"),
    "law1":  ("LAW", "Pháp luật đại cương"),
    "eng1":  ("ENG", "Anh văn 1"),
    "eng2":  ("ENG", "Anh văn 2"),
    "eng3":  ("ENG", "Anh văn chuyên ngành"),
    "phe1":  ("PHE", "Giáo dục thể chất 1"),
    "phe2":  ("PHE", "Giáo dục thể chất 2"),
    "skl1":  ("SKL", "Kỹ năng mềm và khởi nghiệp"),
    "env1":  ("ENV", "Môi trường và phát triển bền vững"),
    # --- Math & science foundation (shared) ---
    "mat_la": ("MAT", "Đại số tuyến tính"),
    "mat_c1": ("MAT", "Giải tích 1"),
    "mat_c2": ("MAT", "Giải tích 2"),
    "mat_dm": ("MAT", "Toán rời rạc"),
    "mat_pr": ("MAT", "Xác suất và thống kê"),
    "mat_nm": ("MAT", "Phương pháp tính"),
    "phy_1":  ("PHY", "Vật lý đại cương"),
    # --- CS core (shared) ---
    "cs_ip":  ("CSC", "Nhập môn lập trình"),
    "cs_pt":  ("CSC", "Kỹ thuật lập trình"),
    "cs_oop": ("CSC", "Lập trình hướng đối tượng"),
    "cs_ds":  ("CSC", "Cấu trúc dữ liệu và giải thuật"),
    "cs_db":  ("CSC", "Cơ sở dữ liệu"),
    "cs_os":  ("CSC", "Hệ điều hành"),
    "cs_net": ("CSC", "Mạng máy tính"),
    "cs_aa":  ("CSC", "Phân tích và thiết kế thuật toán"),
    "cs_ca":  ("CSC", "Kiến trúc máy tính"),
    "cs_se":  ("CSC", "Nhập môn công nghệ phần mềm"),
    "cs_web": ("CSC", "Lập trình ứng dụng web"),
    # --- Machine-learning family (shared by Data Science + AI) ---
    "ml_ml":  ("MLN", "Học máy"),
    "ml_dl":  ("MLN", "Học sâu"),
    "ml_nlp": ("MLN", "Xử lý ngôn ngữ tự nhiên"),
    "ml_cv":  ("MLN", "Thị giác máy tính"),
    # --- Data Science specific ---
    "ds_im":   ("DSC", "Nhập môn Khoa học dữ liệu"),
    "ds_py":   ("DSC", "Lập trình Python cho Khoa học dữ liệu"),
    "ds_viz":  ("DSC", "Trực quan hóa dữ liệu"),
    "ds_stat2":("DSC", "Thống kê nhiều chiều"),
    "ds_dm":   ("DSC", "Khai phá dữ liệu"),
    "ds_big":  ("DSC", "Xử lý dữ liệu lớn"),
    "ds_ts":   ("DSC", "Phân tích chuỗi thời gian"),
    "ds_rec":  ("DSC", "Hệ thống khuyến nghị"),
    "ds_cloud":("DSC", "Điện toán đám mây cho dữ liệu"),
    "ds_opt":  ("DSC", "Tối ưu hóa"),
    "ds_eth":  ("DSC", "Đạo đức và quản trị dữ liệu"),
    "ds_bi":   ("DSC", "Trí tuệ doanh nghiệp (BI)"),
    "ds_mlops":("DSC", "MLOps và triển khai mô hình"),
    "ds_graph":("DSC", "Phân tích mạng và đồ thị"),
    "ds_nosql":("DSC", "Cơ sở dữ liệu NoSQL"),
    "ds_gen":  ("DSC", "Mô hình sinh và AI tạo sinh"),
    "ds_cap1": ("DSC", "Đồ án Khoa học dữ liệu 1"),
    "ds_cap2": ("DSC", "Đồ án Khoa học dữ liệu 2 (Khóa luận)"),
    "ds_intern":("DSC", "Thực tập doanh nghiệp"),
    # --- Information Systems specific ---
    "is_im":   ("ISY", "Nhập môn Hệ thống thông tin"),
    "is_ad":   ("ISY", "Phân tích và thiết kế hệ thống thông tin"),
    "is_dbms": ("ISY", "Hệ quản trị cơ sở dữ liệu"),
    "is_dw":   ("ISY", "Kho dữ liệu và OLAP"),
    "is_erp":  ("ISY", "Hệ thống hoạch định nguồn lực doanh nghiệp (ERP)"),
    "is_bpm":  ("ISY", "Quản lý quy trình nghiệp vụ"),
    "is_pm":   ("ISY", "Quản lý dự án công nghệ thông tin"),
    "is_ec":   ("ISY", "Thương mại điện tử"),
    "is_sec":  ("ISY", "An toàn hệ thống thông tin"),
    "is_dmbiz":("ISY", "Khai thác dữ liệu trong kinh doanh"),
    "is_web2": ("ISY", "Phát triển ứng dụng web doanh nghiệp"),
    "is_mob":  ("ISY", "Phát triển ứng dụng di động"),
    "is_crm":  ("ISY", "Quản trị quan hệ khách hàng (CRM)"),
    "is_bi":   ("ISY", "Hệ thống thông tin quản lý và BI"),
    "is_audit":("ISY", "Kiểm toán hệ thống thông tin"),
    "is_cloud":("ISY", "Điện toán đám mây và dịch vụ"),
    "is_econ": ("ISY", "Kinh tế học cho hệ thống thông tin"),
    "is_acc":  ("ISY", "Nguyên lý kế toán"),
    "is_org":  ("ISY", "Hành vi tổ chức và quản trị"),
    "is_ux":   ("ISY", "Thiết kế trải nghiệm người dùng"),
    "is_gov":  ("ISY", "Quản trị công nghệ thông tin doanh nghiệp"),
    "is_cap1": ("ISY", "Đồ án Hệ thống thông tin 1"),
    "is_cap2": ("ISY", "Đồ án Hệ thống thông tin 2 (Khóa luận)"),
    "is_intern":("ISY", "Thực tập doanh nghiệp"),
    # --- Artificial Intelligence specific ---
    "ai_im":   ("AIM", "Nhập môn Trí tuệ nhân tạo"),
    "ai_py":   ("AIM", "Lập trình Python cho Trí tuệ nhân tạo"),
    "ai_logic":("AIM", "Logic toán cho Trí tuệ nhân tạo"),
    "ai_search":("AIM","Tìm kiếm và tối ưu hóa"),
    "ai_kr":   ("AIM", "Biểu diễn tri thức và suy diễn"),
    "ai_nn":   ("AIM", "Mạng nơ-ron nhân tạo"),
    "ai_rl":   ("AIM", "Học tăng cường"),
    "ai_es":   ("AIM", "Hệ chuyên gia"),
    "ai_robot":("AIM", "Robot học"),
    "ai_speech":("AIM","Xử lý tiếng nói"),
    "ai_game": ("AIM", "Lý thuyết trò chơi và tác tử"),
    "ai_opt":  ("AIM", "Tối ưu hóa lồi"),
    "ai_big":  ("AIM", "Dữ liệu lớn cho Trí tuệ nhân tạo"),
    "ai_ts":   ("AIM", "Chuỗi thời gian và dự báo"),
    "ai_ethics":("AIM","Đạo đức và an toàn Trí tuệ nhân tạo"),
    "ai_gen":  ("AIM", "AI tạo sinh và mô hình ngôn ngữ lớn"),
    "ai_xai":  ("AIM", "Trí tuệ nhân tạo khả diễn giải"),
    "ai_cap1": ("AIM", "Đồ án Trí tuệ nhân tạo 1"),
    "ai_cap2": ("AIM", "Đồ án Trí tuệ nhân tạo 2 (Khóa luận)"),
    "ai_intern":("AIM", "Thực tập doanh nghiệp"),
}

# An entry = (course_key, semester, is_required, [prerequisite_course_keys])

# Shared blocks (identical placement across the programs that use them)
GENED = [
    ("phil1", 1, True,  []),
    ("phil2", 2, True,  []),
    ("phil3", 3, True,  []),
    ("hcm1",  3, True,  []),
    ("hist1", 4, True,  []),
    ("law1",  2, False, []),
    ("eng1",  1, True,  []),
    ("eng2",  2, True,  ["eng1"]),
    ("eng3",  4, False, ["eng2"]),
    ("phe1",  1, False, []),
    ("phe2",  2, False, ["phe1"]),
    ("skl1",  5, False, []),
    ("env1",  6, False, []),
]
MATH = [
    ("mat_la", 1, True,  []),
    ("mat_c1", 1, True,  []),
    ("mat_c2", 2, True,  ["mat_c1"]),
    ("mat_dm", 2, True,  []),
    ("mat_pr", 3, True,  ["mat_c2"]),
    ("mat_nm", 3, False, ["mat_c1"]),
    ("phy_1",  2, False, []),
]
CSCORE = [
    ("cs_ip",  1, True,  []),
    ("cs_pt",  2, True,  ["cs_ip"]),
    ("cs_oop", 3, True,  ["cs_pt"]),
    ("cs_ds",  3, True,  ["cs_pt"]),
    ("cs_db",  4, True,  ["cs_ds"]),
    ("cs_os",  4, True,  ["cs_pt"]),
    ("cs_net", 5, True,  ["cs_os"]),
    ("cs_aa",  4, True,  ["cs_ds", "mat_dm"]),
    ("cs_ca",  3, False, []),
    ("cs_se",  5, True,  ["cs_oop"]),
    ("cs_web", 5, False, ["cs_db"]),
]

# Machine-learning family shared by Data Science and AI
ML_SHARED = [
    ("ml_ml",  5, True,  ["mat_pr", "mat_la"]),
    ("ml_dl",  6, True,  ["ml_ml"]),
    ("ml_nlp", 7, False, ["ml_ml"]),
    ("ml_cv",  7, False, ["ml_dl"]),
]

DS_SPECIFIC = [
    ("ds_im",    3, True,  []),
    ("ds_py",    3, True,  ["cs_pt"]),
    ("ds_viz",   4, True,  ["ds_py"]),
    ("ds_stat2", 4, True,  ["mat_pr"]),
    ("ds_dm",    5, True,  ["cs_db", "mat_pr"]),
    ("ds_big",   6, True,  ["cs_db"]),
    ("ds_opt",   5, False, ["mat_la"]),
    ("ds_ts",    6, False, ["mat_pr"]),
    ("ds_rec",   7, False, ["ml_ml"]),
    ("ds_cloud", 7, False, ["ds_big"]),
    ("ds_eth",   6, False, []),
    ("ds_bi",    6, False, ["cs_db"]),
    ("ds_mlops", 8, False, ["ml_dl"]),
    ("ds_graph", 7, False, ["cs_ds"]),
    ("ds_nosql", 5, False, ["cs_db"]),
    ("ds_gen",   8, False, ["ml_dl"]),
    ("ds_cap1",  7, True,  ["ml_ml"]),
    ("ds_cap2",  8, True,  ["ds_cap1"]),
    ("ds_intern",8, True,  []),
]

IS_SPECIFIC = [
    ("is_im",    2, True,  []),
    ("is_econ",  3, False, []),
    ("is_acc",   3, False, []),
    ("is_org",   4, False, []),
    ("is_ad",    4, True,  ["is_im"]),
    ("is_dbms",  5, True,  ["cs_db"]),
    ("is_dw",    5, True,  ["cs_db"]),
    ("is_bpm",   5, True,  ["is_im"]),
    ("is_ux",    5, False, []),
    ("is_erp",   6, True,  ["is_ad"]),
    ("is_pm",    6, True,  ["cs_se"]),
    ("is_sec",   6, True,  ["cs_net"]),
    ("is_ec",    6, False, ["cs_web"]),
    ("is_dmbiz", 6, False, ["cs_db"]),
    ("is_web2",  6, False, ["cs_web"]),
    ("is_bi",    7, True,  ["is_dw"]),
    ("is_mob",   7, False, ["cs_oop"]),
    ("is_crm",   7, False, ["is_erp"]),
    ("is_audit", 7, False, ["is_sec"]),
    ("is_cloud", 7, False, ["cs_net"]),
    ("is_gov",   8, False, ["is_pm"]),
    ("is_cap1",  7, True,  ["is_ad"]),
    ("is_cap2",  8, True,  ["is_cap1"]),
    ("is_intern",8, True,  []),
]

AI_SPECIFIC = [
    ("ai_py",     3, True,  ["cs_pt"]),
    ("ai_logic",  4, True,  ["mat_dm"]),
    ("ai_im",     4, True,  ["cs_ds"]),
    ("ai_kr",     5, True,  ["ai_im"]),
    ("ai_search", 5, True,  ["cs_aa"]),
    ("ai_nn",     6, False, ["ml_ml"]),
    ("ai_opt",    5, False, ["mat_la"]),
    ("ai_es",     6, False, ["ai_kr"]),
    ("ai_big",    6, False, ["cs_db"]),
    ("ai_game",   6, False, ["ai_search"]),
    ("ai_rl",     6, True,  ["ml_ml"]),
    ("ai_robot",  7, False, ["ai_rl"]),
    ("ai_speech", 7, False, ["ml_dl"]),
    ("ai_ts",     7, False, ["mat_pr"]),
    ("ai_ethics", 7, False, []),
    ("ai_gen",    7, False, ["ml_dl"]),
    ("ai_xai",    8, False, ["ml_dl"]),
    ("ai_cap1",   7, True,  ["ml_ml"]),
    ("ai_cap2",   8, True,  ["ai_cap1"]),
    ("ai_intern", 8, True,  []),
]

PROGRAMS = {
    1: GENED + MATH + CSCORE + ML_SHARED + DS_SPECIFIC,   # Data Science
    2: GENED + MATH + CSCORE + IS_SPECIFIC,               # Information Systems
    3: GENED + MATH + CSCORE + ML_SHARED + AI_SPECIFIC,   # Artificial Intelligence
}


def credits_for(key, is_required):
    """Required -> 3 or 4 ; Optional -> 1 or 2. Deterministic, varied by course key."""
    h = sum(ord(c) for c in key)
    if is_required:
        return 3 + (h % 2)      # 3 or 4
    return 1 + (h % 2)          # 1 or 2


def main():
    # --- Validate every program: prereqs exist & are in a strictly earlier semester ---
    for pid, entries in PROGRAMS.items():
        sem_of = {k: s for (k, s, r, p) in entries}
        keys = set(sem_of)
        dup = [k for k in [e[0] for e in entries]]
        assert len(dup) == len(set(dup)), f"Program {pid} has a duplicate course"
        for (k, s, r, prereqs) in entries:
            for pre in prereqs:
                assert pre in keys, f"Program {pid}: {k} prereq {pre} not in program"
                assert sem_of[pre] < s, (
                    f"Program {pid}: {k}(sem {s}) prereq {pre}(sem {sem_of[pre]}) "
                    f"not strictly earlier -> would risk a cycle")
        n = len(entries)
        assert 50 <= n <= 60, f"Program {pid} has {n} courses (need 50-60)"

    # --- Assign course ids (per-prefix sequence) in catalog declaration order ---
    used_keys = set()
    for entries in PROGRAMS.values():
        used_keys.update(k for (k, *_ ) in entries)

    seq = {}
    course_pk = {}        # key -> courses.id
    course_code = {}      # key -> course_id business key (PRX#####)
    course_rows = []
    cid = 0
    for key, (prefix, name) in CATALOG.items():
        if key not in used_keys:
            continue
        cid += 1
        seq[prefix] = seq.get(prefix, 0) + 1
        code = f"{prefix}{seq[prefix]:05d}"
        course_pk[key] = cid
        course_code[key] = code
        course_rows.append((cid, code, name))

    # --- Build course_majors rows ---
    cm_rows = []           # (cm_id, course_pk, program_id, credits, semester, is_required)
    cm_id_of = {}          # (pid, key) -> cm_id
    cmid = 0
    totals = {}            # pid -> [required_credits, optional_credits]
    for pid, entries in PROGRAMS.items():
        totals[pid] = [0, 0]
        for (k, s, r, prereqs) in entries:
            cmid += 1
            cr = credits_for(k, r)
            cm_rows.append((cmid, course_pk[k], pid, cr, s, r))
            cm_id_of[(pid, k)] = cmid
            totals[pid][0 if r else 1] += cr

    # --- Build prerequisite rows ---
    prereq_rows = []       # (course_major_id, prerequisite_course_id)
    for pid, entries in PROGRAMS.items():
        for (k, s, r, prereqs) in entries:
            for pre in prereqs:
                prereq_rows.append((cm_id_of[(pid, k)], course_pk[pre]))

    # ----------------------------------------------------------------- emit SQL
    def esc(s):
        return s.replace("'", "''")

    out = []
    out.append("-- Generated curriculum seed for the 3 existing major-programs.")
    out.append("-- Idempotent: clears curriculum tables, then reseeds. Run inside one tx.")
    out.append("BEGIN;")
    out.append("")
    out.append("TRUNCATE TABLE course_major_prerequisites, open_courses, course_majors, courses "
               "RESTART IDENTITY CASCADE;")
    out.append("")

    out.append("INSERT INTO courses (id, course_id, name) VALUES")
    vals = [f"  ({i}, '{code}', '{esc(name)}')" for (i, code, name) in course_rows]
    out.append(",\n".join(vals) + ";")
    out.append("")

    out.append("INSERT INTO course_majors "
               "(id, course_id, major_program_id, credits, program_semester, is_required) VALUES")
    vals = [f"  ({i}, {cpk}, {pid}, {cr}, {sem}, {str(bool(r)).upper()})"
            for (i, cpk, pid, cr, sem, r) in cm_rows]
    out.append(",\n".join(vals) + ";")
    out.append("")

    out.append("INSERT INTO course_major_prerequisites (course_major_id, prerequisite_course_id) VALUES")
    vals = [f"  ({cm}, {pc})" for (cm, pc) in prereq_rows]
    out.append(",\n".join(vals) + ";")
    out.append("")

    out.append("-- Recompute program credit totals from the seeded curriculum.")
    for pid in sorted(totals):
        req, opt = totals[pid]
        out.append(f"UPDATE major_programs SET total_required_credit = {req}, "
                   f"total_optional_credit = {opt} WHERE id = {pid};")
    out.append("")
    out.append("-- Fix sequences so future app inserts continue after the seeded ids.")
    out.append("SELECT setval('courses_id_seq', (SELECT MAX(id) FROM courses));")
    out.append("SELECT setval('course_majors_id_seq', (SELECT MAX(id) FROM course_majors));")
    out.append("")
    out.append("COMMIT;")

    sql = "\n".join(out) + "\n"
    with open("/tmp/claude-1000/-home-tmanh-hcmus-student-course-management/694f907c-bbdf-4226-9736-d94682d5606b/scratchpad/curriculum_seed.sql", "w") as f:
        f.write(sql)

    # ------------------------------------------------------------- console report
    print(f"distinct courses : {len(course_rows)}")
    print(f"course_majors    : {len(cm_rows)}")
    print(f"prerequisites    : {len(prereq_rows)}")
    for pid, entries in PROGRAMS.items():
        req = sum(1 for e in entries if e[2])
        opt = len(entries) - req
        print(f"  program {pid}: {len(entries)} courses "
              f"({req} required / {opt} optional), "
              f"credits required={totals[pid][0]} optional={totals[pid][1]}")
    # shared-course check
    appears = {}
    for pid, entries in PROGRAMS.items():
        for (k, *_ ) in entries:
            appears.setdefault(k, []).append(pid)
    shared = {k: v for k, v in appears.items() if len(v) > 1}
    print(f"shared courses (in >1 program): {len(shared)}")


if __name__ == "__main__":
    main()
