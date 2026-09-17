/*
 * Workly site behaviour: language switch, theme switch, scroll reveal.
 *
 * The page is a single static file, so all copy lives here as a dictionary and
 * every translatable element carries a data-i18n key. Adding a language means
 * adding one object — the same idea as the app's resource files.
 */
(function () {
  'use strict';

  var COPY = {
    en: {
      title: 'Workly — Personal work hours and earnings tracker',
      'nav.features': 'Features',
      'nav.screens': 'Screens',
      'nav.privacy': 'Privacy',
      'nav.tech': 'Tech',
      'nav.download': 'Download',
      'nav.langAria': 'Language',
      'nav.themeAria': 'Switch theme',
      'hero.eyebrow': 'Offline · No account · No ads',
      'hero.title': 'Your work hours, quietly kept.',
      'hero.lede': 'Workly is a personal work hours and earnings tracker for people paid by the hour. Tap start, get on with your day, tap finish — the app works out the rest.',
      'hero.ctaPrimary': 'Get the app',
      'hero.ctaSecondary': 'View source',
      'hero.stat1Value': '0',
      'hero.stat1Label': 'permissions requested',
      'hero.stat2Value': '3',
      'hero.stat2Label': 'languages',
      'hero.stat3Value': '100%',
      'hero.stat3Label': 'offline',
      'mock.greeting': 'Good evening',
      'mock.date': 'Wednesday, September 16',
      'mock.workingNow': 'WORKING NOW',
      'mock.started': 'Started at 18:26',
      'mock.earnings': 'EARNINGS SO FAR',
      'mock.finish': 'Finish work',
      'mock.thisWeek': 'THIS WEEK',
      'mock.worked': 'Worked',
      'mock.income': 'Income',
      'mock.target': '32h 18m of 40h',
      'mock.workedToday': 'WORKED TODAY',
      'mock.earnedToday': 'EARNED TODAY',
      'mock.start': 'Start work',
      'mock.logManually': 'Log time manually',
      'mock.records': 'Records',
      'mock.sep16': 'September 16 · 8h 00m · ¥12,800',
      'mock.sep15': 'September 15 · 6h 30m · ¥10,400',
      'mock.sep14': 'September 14 · 4h 00m · ¥6,400',
      'mock.statistics': 'Statistics',
      'mock.totalTime': 'TOTAL WORK TIME',
      'mock.totalIncome': 'TOTAL INCOME',
      'mock.chartHint': 'Mon 4h · Tue 6.5h · Wed 5h · Thu 8h…',
      'features.title': 'Everything a private time tracker needs',
      'features.lede': 'And nothing it does not. No feeds, no social, no upsell.',
      'features.f1Title': 'One tap to start',
      'features.f1Body': 'The running session is written to the database immediately, so closing the app, locking the screen or a reboot never loses it.',
      'features.f2Title': 'Finish with a summary',
      'features.f2Body': 'Confirm the end time, break, rate, work type and note before saving. Everything is editable in one screen.',
      'features.f3Title': 'Charts that state numbers',
      'features.f3Body': 'Values are drawn on the bars and every data point is also listed underneath, so a month of work is readable at a glance.',
      'features.f4Title': 'Calendar with day controls',
      'features.f4Body': 'Long-press any day to record hours for it, mark it as a rest day, or give it its own target. Every choice is remembered.',
      'features.f5Title': 'Your own background',
      'features.f5Body': 'Pick a colour or your own picture, blur it from 0% to 100%, and let the app take its accent from the image.',
      'features.f6Title': 'Schedule and targets',
      'features.f6Body': 'Set a weekly hours target and your rest days; the dashboard and statistics show progress against them.',
      'features.f7Title': 'Your data, portable',
      'features.f7Body': 'Export CSV for a spreadsheet or a full JSON backup, and import it back with automatic duplicate detection.',
      'features.f8Title': 'English, 日本語, 中文',
      'features.f8Body': 'Switch language inside the app, or from the system "App language" screen on Android 13 and newer.',
      'screens.title': 'Designed to be read in three seconds',
      'screens.lede': 'Big numbers, generous space, and colour used only where it means something.',
      'screens.home': 'Home',
      'screens.records': 'Records',
      'screens.statistics': 'Statistics',
      'screens.note': "These previews are HTML recreations of the app's real layout. Render the @Preview composables in the repository for device screenshots.",
      'privacy.title': 'Nothing leaves your phone',
      'privacy.lede': 'Workly asks for no permissions at all. There is no internet permission in the manifest, which means the app is technically incapable of sending your data anywhere.',
      'privacy.t1': 'No account, no sign-in, no email',
      'privacy.t2': 'No cloud sync and no server of any kind',
      'privacy.t3': 'No ads, no analytics, no trackers',
      'privacy.t4': 'Backups are plain files you choose where to save',
      'privacy.codeNote': "From the app's AndroidManifest.xml",
      'tech.title': 'Built with modern Android',
      'tech.lede': 'A single small module, readable by someone still learning.',
      'tech.q1': 'JVM unit tests',
      'tech.q2': 'instrumented tests',
      'tech.q3': 'lint errors',
      'tech.q4': 'CI workflows',
      'download.title': 'Get Workly',
      'download.lede': 'Workly is free and open source under the MIT licence. Build it yourself, or grab the debug APK from the latest CI run.',
      'download.apk': 'Download the APK',
      'download.repo': 'Browse the repository',
      'footer.readme': 'README',
      'footer.readmeZh': '中文说明',
      'footer.license': 'MIT License',
      'footer.note': 'Made with Kotlin and Jetpack Compose.'
    },

    zh: {
      title: 'Workly — 个人工时与收入记录',
      'nav.features': '功能',
      'nav.screens': '界面',
      'nav.privacy': '隐私',
      'nav.tech': '技术',
      'nav.download': '下载',
      'nav.langAria': '语言',
      'nav.themeAria': '切换主题',
      'hero.eyebrow': '离线 · 无需账号 · 无广告',
      'hero.title': '安静地记录你的工时。',
      'hero.lede': 'Workly 是给按小时计酬的人用的个人工时与收入记录工具。点一下开始，忙你的事，再点一下结束，剩下的交给它。',
      'hero.ctaPrimary': '获取应用',
      'hero.ctaSecondary': '查看源码',
      'hero.stat1Value': '0',
      'hero.stat1Label': '项权限申请',
      'hero.stat2Value': '3',
      'hero.stat2Label': '种语言',
      'hero.stat3Value': '100%',
      'hero.stat3Label': '离线可用',
      'mock.greeting': '晚上好',
      'mock.date': '9月16日 星期三',
      'mock.workingNow': '正在工作',
      'mock.started': '18:26 开始',
      'mock.earnings': '当前收入',
      'mock.finish': '结束工作',
      'mock.thisWeek': '本周',
      'mock.worked': '工时',
      'mock.income': '收入',
      'mock.target': '32小时18分 / 40小时',
      'mock.workedToday': '今日工时',
      'mock.earnedToday': '今日收入',
      'mock.start': '开始工作',
      'mock.logManually': '直接记录工时',
      'mock.records': '记录',
      'mock.sep16': '9月16日 · 8小时00分 · ¥12,800',
      'mock.sep15': '9月15日 · 6小时30分 · ¥10,400',
      'mock.sep14': '9月14日 · 4小时00分 · ¥6,400',
      'mock.statistics': '统计',
      'mock.totalTime': '总工时',
      'mock.totalIncome': '总收入',
      'mock.chartHint': '周一 4小时 · 周二 6.5小时 · 周三 5小时…',
      'features.title': '一个私人计时工具该有的，都在这里',
      'features.lede': '没有多余的东西：没有信息流、没有社交、没有推销。',
      'features.f1Title': '一键开始',
      'features.f1Body': '进行中的记录会立刻写入数据库，所以关闭 App、锁屏或重启手机都不会丢。',
      'features.f2Title': '结束时确认汇总',
      'features.f2Body': '保存前可以确认结束时间、休息、时薪、工作类型和备注，全部在同一屏里改完。',
      'features.f3Title': '图表直接把数字写出来',
      'features.f3Body': '数值画在柱子上，同时每个数据点都会在下方逐条列出，一个月的数据一眼看完。',
      'features.f4Title': '日历上按天调整',
      'features.f4Body': '长按任意日期即可记录当天工时、设为休息日，或单独指定当天目标。每次选择都会被记住。',
      'features.f5Title': '你自己的背景',
      'features.f5Body': '选一个纯色或自己的图片，模糊程度 0%–100% 可调，还能让配色取自图片主色调。',
      'features.f6Title': '工作安排与目标',
      'features.f6Body': '设置每周目标工时和休息日，首页与统计会显示完成进度。',
      'features.f7Title': '数据可以带走',
      'features.f7Body': '导出 CSV 给表格软件，或导出完整 JSON 备份；再导入时会自动去重。',
      'features.f8Title': '英文、日本語、中文',
      'features.f8Body': '在应用内切换语言，Android 13 及以上也可以从系统的「应用语言」里改。',
      'screens.title': '三秒之内就能看懂',
      'screens.lede': '大数字、充足留白，颜色只在有意义的地方出现。',
      'screens.home': '首页',
      'screens.records': '记录',
      'screens.statistics': '统计',
      'screens.note': '以上是用 HTML 还原的应用布局预览。真机截图请在仓库中用 Android Studio 渲染 @Preview 生成。',
      'privacy.title': '什么都不会离开你的手机',
      'privacy.lede': 'Workly 不申请任何权限。清单文件里没有网络权限，也就是说它在技术上无法把你的数据发到任何地方。',
      'privacy.t1': '没有账号、没有登录、不需要邮箱',
      'privacy.t2': '没有云同步，也没有任何服务器',
      'privacy.t3': '没有广告、没有统计上报、没有追踪',
      'privacy.t4': '备份就是普通文件，存放在你自己选的位置',
      'privacy.codeNote': '摘自应用的 AndroidManifest.xml',
      'tech.title': '用现代 Android 技术构建',
      'tech.lede': '单个小模块，刚学 Android 的人也能读懂。',
      'tech.q1': '个 JVM 单元测试',
      'tech.q2': '个仪器化测试',
      'tech.q3': '个 lint 错误',
      'tech.q4': '套 CI 工作流',
      'download.title': '获取 Workly',
      'download.lede': 'Workly 基于 MIT 许可证免费开源。你可以自己构建，也可以直接从最近的 CI 构建里下载 debug APK。',
      'download.apk': '下载 APK',
      'download.repo': '浏览仓库',
      'footer.readme': 'README（英文）',
      'footer.readmeZh': '中文说明',
      'footer.license': 'MIT 许可证',
      'footer.note': '使用 Kotlin 与 Jetpack Compose 构建。'
    },

    ja: {
      title: 'Workly — 個人の労働時間と収入の記録',
      'nav.features': '機能',
      'nav.screens': '画面',
      'nav.privacy': 'プライバシー',
      'nav.tech': '技術',
      'nav.download': 'ダウンロード',
      'nav.langAria': '言語',
      'nav.themeAria': 'テーマを切り替え',
      'hero.eyebrow': 'オフライン · アカウント不要 · 広告なし',
      'hero.title': '労働時間を、静かに記録。',
      'hero.lede': 'Workly は時給で働く人のための、労働時間と収入の記録アプリです。開始を押して作業に戻り、終了を押すだけ。あとは自動で計算します。',
      'hero.ctaPrimary': 'アプリを入手',
      'hero.ctaSecondary': 'ソースを見る',
      'hero.stat1Value': '0',
      'hero.stat1Label': '要求する権限',
      'hero.stat2Value': '3',
      'hero.stat2Label': '対応言語',
      'hero.stat3Value': '100%',
      'hero.stat3Label': 'オフライン',
      'mock.greeting': 'こんばんは',
      'mock.date': '9月16日 水曜日',
      'mock.workingNow': '作業中',
      'mock.started': '18:26 に開始',
      'mock.earnings': '現在の収入',
      'mock.finish': '作業を終了',
      'mock.thisWeek': '今週',
      'mock.worked': '労働時間',
      'mock.income': '収入',
      'mock.target': '32時間18分 / 40時間',
      'mock.workedToday': '今日の労働時間',
      'mock.earnedToday': '今日の収入',
      'mock.start': '作業を開始',
      'mock.logManually': '手動で記録',
      'mock.records': '記録',
      'mock.sep16': '9月16日 · 8時間00分 · ¥12,800',
      'mock.sep15': '9月15日 · 6時間30分 · ¥10,400',
      'mock.sep14': '9月14日 · 4時間00分 · ¥6,400',
      'mock.statistics': '統計',
      'mock.totalTime': '総労働時間',
      'mock.totalIncome': '総収入',
      'mock.chartHint': '月 4時間 · 火 6.5時間 · 水 5時間 · 木 8時間…',
      'features.title': '個人用の記録アプリに必要なものだけ',
      'features.lede': 'フィードも、SNS も、売り込みもありません。',
      'features.f1Title': 'ワンタップで開始',
      'features.f1Body': '進行中の記録はすぐにデータベースへ書き込まれるため、アプリを閉じても、画面をロックしても、再起動しても失われません。',
      'features.f2Title': '終了時に内容を確認',
      'features.f2Body': '保存前に終了時刻・休憩・時給・仕事の種類・メモを確認できます。すべて同じ画面で編集できます。',
      'features.f3Title': '数字を書くグラフ',
      'features.f3Body': '値は棒の上に描かれ、さらに各データが下に一覧表示されるので、1か月分も一目で読めます。',
      'features.f4Title': '日付ごとに調整できるカレンダー',
      'features.f4Body': '日付を長押しすると、その日の工数を記録し、休みの日にするか、その日だけの目標を設定できます。選択はすべて保存されます。',
      'features.f5Title': '自分の背景',
      'features.f5Body': '単色か自分の画像を選び、ぼかしを 0%〜100% で調整。画像の主要色を配色に使うこともできます。',
      'features.f6Title': 'スケジュールと目標',
      'features.f6Body': '週の目標時間と休みの曜日を設定すると、ホームと統計に進捗が表示されます。',
      'features.f7Title': 'データは持ち出せる',
      'features.f7Body': '表計算向けの CSV か、完全な JSON バックアップとして書き出し、重複を検出しながら復元できます。',
      'features.f8Title': 'English, 日本語, 中文',
      'features.f8Body': 'アプリ内で言語を切り替えられます。Android 13 以降ではシステムの「アプリの言語」からも変更できます。',
      'screens.title': '3秒で読めるように設計',
      'screens.lede': '大きな数字、十分な余白、意味のある場所にだけ色を使います。',
      'screens.home': 'ホーム',
      'screens.records': '記録',
      'screens.statistics': '統計',
      'screens.note': '上のプレビューはアプリの実際のレイアウトを HTML で再現したものです。実機のスクリーンショットはリポジトリの @Preview から書き出してください。',
      'privacy.title': 'データは端末から出ません',
      'privacy.lede': 'Workly は権限を一切要求しません。マニフェストにインターネット権限がないため、データをどこかへ送ることは技術的にできません。',
      'privacy.t1': 'アカウントもログインもメールアドレスも不要',
      'privacy.t2': 'クラウド同期もサーバーもありません',
      'privacy.t3': '広告・解析・トラッカーなし',
      'privacy.t4': 'バックアップは保存先を自分で選べるただのファイル',
      'privacy.codeNote': 'アプリの AndroidManifest.xml より',
      'tech.title': 'モダンな Android で構築',
      'tech.lede': '小さな単一モジュール。学びながら読める構成です。',
      'tech.q1': '件の JVM ユニットテスト',
      'tech.q2': '件の計測テスト',
      'tech.q3': '件の lint エラー',
      'tech.q4': '本の CI ワークフロー',
      'download.title': 'Workly を入手',
      'download.lede': 'Workly は MIT ライセンスの無料オープンソースです。自分でビルドするか、最新の CI から debug APK を取得してください。',
      'download.apk': 'APK をダウンロード',
      'download.repo': 'リポジトリを見る',
      'footer.readme': 'README（英語）',
      'footer.readmeZh': '中文说明',
      'footer.license': 'MIT ライセンス',
      'footer.note': 'Kotlin と Jetpack Compose で作られています。'
    }
  };

  var STORAGE_LANG = 'workly.lang';
  var STORAGE_THEME = 'workly.theme';

  function detectLanguage() {
    var saved = null;
    try { saved = localStorage.getItem(STORAGE_LANG); } catch (e) { /* private mode */ }
    if (saved && COPY[saved]) return saved;

    var nav = (navigator.language || 'en').toLowerCase();
    if (nav.indexOf('zh') === 0) return 'zh';
    if (nav.indexOf('ja') === 0) return 'ja';
    return 'en';
  }

  function applyLanguage(code) {
    var dict = COPY[code] || COPY.en;
    document.documentElement.lang = code;

    document.querySelectorAll('[data-i18n]').forEach(function (el) {
      var value = dict[el.getAttribute('data-i18n')];
      if (value) el.textContent = value;
    });

    document.querySelectorAll('[data-i18n-aria]').forEach(function (el) {
      var value = dict[el.getAttribute('data-i18n-aria')];
      if (value) el.setAttribute('aria-label', value);
    });

    if (dict.title) document.title = dict.title;

    var select = document.getElementById('lang');
    if (select) select.value = code;

    try { localStorage.setItem(STORAGE_LANG, code); } catch (e) { /* ignore */ }
  }

  function applyTheme(mode) {
    document.documentElement.dataset.theme = mode;
    try { localStorage.setItem(STORAGE_THEME, mode); } catch (e) { /* ignore */ }
  }

  function detectTheme() {
    var saved = null;
    try { saved = localStorage.getItem(STORAGE_THEME); } catch (e) { /* private mode */ }
    if (saved === 'light' || saved === 'dark') return saved;
    return 'auto';
  }

  function init() {
    applyLanguage(detectLanguage());
    applyTheme(detectTheme());

    var langSelect = document.getElementById('lang');
    if (langSelect) {
      langSelect.addEventListener('change', function () {
        applyLanguage(langSelect.value);
      });
    }

    var themeButton = document.getElementById('theme');
    if (themeButton) {
      themeButton.addEventListener('click', function () {
        // auto -> dark -> light -> auto, so the system setting is always reachable
        var current = document.documentElement.dataset.theme || 'auto';
        var next = current === 'auto' ? 'dark' : current === 'dark' ? 'light' : 'auto';
        applyTheme(next);
      });
    }

    var nav = document.querySelector('.nav');
    var onScroll = function () {
      if (nav) nav.classList.toggle('is-stuck', window.scrollY > 8);
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });

    var reveals = document.querySelectorAll('.reveal');
    if (!('IntersectionObserver' in window)) {
      reveals.forEach(function (el) { el.classList.add('is-visible'); });
      return;
    }
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        }
      });
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });
    reveals.forEach(function (el) { observer.observe(el); });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
