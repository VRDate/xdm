package xdman.ui.laf;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLabelUI;

public class XDMLabelUI extends BasicLabelUI {

	public static ComponentUI createUI(JComponent c) {
		return new XDMLabelUI();
	}

//	@Override
//	public void paint(Graphics g, JComponent c) {
//		Logger.log("called label paint");
//		Graphics2D g2d = (Graphics2D) g;
//		Toolkit tk = Toolkit.getDefaultToolkit();
//		Map map = (Map) (tk.getDesktopProperty("awt.font.desktophints"));
//		if (map != null) {
//			g2d.addRenderingHints(map);
//		}
//		super.paint(g2d, c);
//	}
}
