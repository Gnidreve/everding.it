import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _paperYellow = Color(0xFFFFF3B0);
const _inkColor = Color(0xFF2E2A1F);
const _lineColor = Color(0x33000000);
const _marginColor = Color(0xFFE2857A);
const _fontSize = 19.0;
const _lineHeight = 30.0;
const _topOffset = 26.0;
const _marginX = 28.0;
const _prefsKey = 'note_text';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: _paperYellow,
      statusBarIconBrightness: Brightness.dark,
      systemNavigationBarColor: _paperYellow,
      systemNavigationBarIconBrightness: Brightness.dark,
    ),
  );
  runApp(const NotepadApp());
}

class NotepadApp extends StatelessWidget {
  const NotepadApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Notizblock',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        scaffoldBackgroundColor: _paperYellow,
        textSelectionTheme: const TextSelectionThemeData(
          cursorColor: _inkColor,
          selectionColor: Color(0x552E2A1F),
        ),
      ),
      home: const NotePage(),
    );
  }
}

/// Einzige Seite der App: ein endlos scrollbares, gelbes Notizblatt.
/// Jede Änderung wird sofort persistiert (SharedPreferences, überlebt
/// App-Kill/Neustart), zusätzlich beim App-Pause nochmal zur Sicherheit.
class NotePage extends StatefulWidget {
  const NotePage({super.key});

  @override
  State<NotePage> createState() => _NotePageState();
}

class _NotePageState extends State<NotePage> with WidgetsBindingObserver {
  final _controller = TextEditingController();
  bool _ready = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _controller.addListener(_save);
    _load();
  }

  Future<void> _load() async {
    final prefs = await SharedPreferences.getInstance();
    _controller.text = prefs.getString(_prefsKey) ?? '';
    if (mounted) setState(() => _ready = true);
  }

  Future<void> _save() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefsKey, _controller.text);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.inactive ||
        state == AppLifecycleState.detached) {
      _save();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _controller.removeListener(_save);
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!_ready) {
      return const Scaffold(backgroundColor: _paperYellow, body: SizedBox.shrink());
    }

    return Scaffold(
      backgroundColor: _paperYellow,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 64),
          child: Stack(
            children: [
              Positioned.fill(
                child: CustomPaint(
                  painter: const _RuledPaper(
                    lineHeight: _lineHeight,
                    topOffset: _topOffset,
                    lineColor: _lineColor,
                    marginColor: _marginColor,
                    marginX: _marginX,
                  ),
                ),
              ),
              ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: MediaQuery.of(context).size.height,
                ),
                child: Padding(
                  padding: const EdgeInsets.only(left: 36),
                  child: TextField(
                    controller: _controller,
                    autofocus: true,
                    maxLines: null,
                    keyboardType: TextInputType.multiline,
                    textInputAction: TextInputAction.newline,
                    cursorColor: _inkColor,
                    style: const TextStyle(
                      fontSize: _fontSize,
                      height: _lineHeight / _fontSize,
                      color: _inkColor,
                    ),
                    decoration: const InputDecoration(
                      border: InputBorder.none,
                      isDense: true,
                      contentPadding: EdgeInsets.zero,
                      hintText: '',
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RuledPaper extends CustomPainter {
  const _RuledPaper({
    required this.lineHeight,
    required this.topOffset,
    required this.lineColor,
    required this.marginColor,
    required this.marginX,
  });

  final double lineHeight;
  final double topOffset;
  final Color lineColor;
  final Color marginColor;
  final double marginX;

  @override
  void paint(Canvas canvas, Size size) {
    final linePaint = Paint()
      ..color = lineColor
      ..strokeWidth = 1;
    var y = topOffset;
    while (y < size.height) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), linePaint);
      y += lineHeight;
    }

    final marginPaint = Paint()
      ..color = marginColor
      ..strokeWidth = 2;
    canvas.drawLine(Offset(marginX, 0), Offset(marginX, size.height), marginPaint);
  }

  @override
  bool shouldRepaint(covariant _RuledPaper oldDelegate) => false;
}
