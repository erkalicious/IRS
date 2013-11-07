/**
 * 
 */
package ISIS.gui;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

/**
 * @author eric
 */
public class HintField extends JTextField {
	private static final long	serialVersionUID	= 1L;
	private boolean				hintShown;
	private boolean				showHint;
	private String				hint;
	
	public HintField() {
		this("");
	}
	
	public HintField(String hint) {
		super(hint);
		this.hint = hint;
		this.hintShown = true;
		this.showHint = true;
		this.addFocusListener(this.new Listener());
	}
	
	public void setShowHint(boolean b) {
		this.showHint = b;
	}
	
	@Override
	public String getText() {
		if (!this.hintShown) return super.getText();
		else return "";
	}
	
	class Listener implements FocusListener {
		
		@Override
		public void focusGained(FocusEvent fe) {
			HintField.this.selectAll();
			if (HintField.this.hintShown) {
				HintField.this.setText("");
			}
			HintField.this.hintShown = false;
		}
		
		@Override
		public void focusLost(FocusEvent fe) {
			HintField.this.setCaretPosition(0);
			if (HintField.this.getText().length() == 0) {
				HintField.this.hintShown = true;
				if (HintField.this.showHint) {
					HintField.this.setText(HintField.this.hint);
				}
			}
		}
	}
}