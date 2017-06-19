package dev.mortus.gui;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import dev.mortus.test.GUITestView;

public class ViewerFrame extends JFrame {

	private static final long serialVersionUID = -3995478660714859610L;

	private ViewerPane viewerPane;

	/**
	 * Create the frame.
	 */
	public ViewerFrame(View view) {
		setTitle("Viewer");
		setSize(new Dimension(800, 800));
		setVisible(true);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		view = new GUITestView(10, 10, 100, 100);
		viewerPane = new ViewerPane(view);
		view.setViewerPane(viewerPane);
		
		viewerPane.setFocusable(true);
		viewerPane.setPreferredSize(new Dimension(1280, 720));
		add(viewerPane);
		pack();
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("Closing");
				ViewerFrame.this.viewerPane.stop();
				ViewerFrame.this.dispose();
			}
		});
		
		viewerPane.start();
	}

}
