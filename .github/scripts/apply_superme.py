from pathlib import Path


def replace_once(path, old, new, missing_error):
    p = Path(path)
    s = p.read_text(encoding="utf-8")
    if new in s:
        return
    if old not in s:
        raise SystemExit(missing_error)
    p.write_text(s.replace(old, new, 1), encoding="utf-8")


ui = Path("TMessagesProj/src/main/java/org/telegram/ui")

# Put Superme gifts into the real Telegram profile gift tab.
p = ui / "ProfileActivity.java"
s = p.read_text(encoding="utf-8")
field_marker = "    public ProfileGiftsView giftsView;\n"
if "private SupermeProfileGiftsView supermeProfileGiftsView;" not in s:
    if field_marker not in s:
        raise SystemExit("Profile gifts field insertion point not found")
    s = s.replace(field_marker, field_marker + "    private SupermeProfileGiftsView supermeProfileGiftsView;\n", 1)
setup_marker = "        sharedMediaLayout.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT));\n        sharedMediaLayout.initBlurCapture((ViewGroup) fragmentView);"
setup_add = """        sharedMediaLayout.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT));
        sharedMediaLayout.initBlurCapture((ViewGroup) fragmentView);
        supermeProfileGiftsView = new SupermeProfileGiftsView(context);
        FrameLayout.LayoutParams supermeGiftsLp = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT);
        supermeGiftsLp.topMargin = dp(54);
        frameLayout.addView(supermeProfileGiftsView, supermeGiftsLp);
        boolean showSupermeGifts = sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_GIFTS;
        supermeProfileGiftsView.setVisibility(showSupermeGifts ? View.VISIBLE : View.GONE);
        if (showSupermeGifts) supermeProfileGiftsView.rebuild();
"""
if "supermeProfileGiftsView = new SupermeProfileGiftsView(context);" not in s:
    if setup_marker not in s:
        raise SystemExit("Profile gifts setup insertion point not found")
    s = s.replace(setup_marker, setup_add, 1)
tab_marker = """            protected void onSelectedTabChanged() {
                updateSelectedMediaTabText();
            }"""
tab_add = """            protected void onSelectedTabChanged() {
                updateSelectedMediaTabText();
                if (supermeProfileGiftsView != null) {
                    boolean show = getSelectedTab() == SharedMediaLayout.TAB_GIFTS;
                    supermeProfileGiftsView.setVisibility(show ? View.VISIBLE : View.GONE);
                    if (show) supermeProfileGiftsView.rebuild();
                }
            }"""
if "boolean show = getSelectedTab() == SharedMediaLayout.TAB_GIFTS;" not in s:
    if tab_marker not in s:
        raise SystemExit("Profile tab callback insertion point not found")
    s = s.replace(tab_marker, tab_add, 1)
p.write_text(s, encoding="utf-8")

# Route Telegram gift purchases through Superme validation/backend.
p = ui / "Gifts/SendGiftSheet.java"
s = p.read_text(encoding="utf-8")
if "import org.telegram.ui.SupermePurchaseApi;" not in s:
    marker = "import org.telegram.ui.ProfileActivity;\n"
    if marker not in s:
        raise SystemExit("SendGiftSheet import insertion point not found")
    s = s.replace(marker, marker + "import org.telegram.ui.SupermePurchaseApi;\n", 1)
old = "StarsController.getInstance(currentAccount).buyStarGift("
new = "SupermePurchaseApi.purchaseStarGift(currentAccount, "
if old not in s and new not in s:
    raise SystemExit("SendGiftSheet Star Gift purchase call not found")
if old in s:
    s = s.replace(old, new, 1)
p.write_text(s, encoding="utf-8")

# Add a native Superme BotFather-style entry to Settings. It opens the real BotFather
# for creation of real Telegram bot accounts; no fake bot token is generated locally.
p = ui / "SettingsActivity.java"
s = p.read_text(encoding="utf-8")
setting_marker = 'items.add(SettingCell.Factory.of(16, 0xFFF38B31, 0xFFE26314, R.drawable.settings_gift, getString(R.string.SendAGift)));'
if "🤖 Bot yaratish" not in s:
    if setting_marker not in s:
        raise SystemExit("Settings BotCreator insertion point not found")
    s = s.replace(
        setting_marker,
        setting_marker + '\n        items.add(SettingCell.Factory.of(26, 0xFF6C63FF, 0xFF4B44CC, R.drawable.settings_gift, "🤖 Bot yaratish"));',
        1,
    )
case_marker = "            case 17:\n"
if "case 26:" not in s:
    if case_marker not in s:
        raise SystemExit("Settings BotCreator click insertion point not found")
    s = s.replace(
        case_marker,
        "            case 26:\n                presentSettingFragment(new BotCreatorActivity());\n                break;\n" + case_marker,
        1,
    )
p.write_text(s, encoding="utf-8")

print("Superme Telegram integration applied successfully")
