/**
 * 
 */
package ISIS.gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * @author eric
 */
public class NumberHintField extends HintField {
	private static final long	serialVersionUID	= 1L;
	
	/**
	 * @pre - none, constructor
	 * @post - object instantiated and returned
	 */
	public NumberHintField() {
		this(null);
	}
	
	/**
	 * @param hint
	 * @pre - string received, constructor
	 * @post - object instantiated and returned
	 */
	public NumberHintField(String hint) {
		super(hint);
		
		((PlainDocument) this.getDocument())
				.setDocumentFilter(new DocumentFilter() {
					private boolean check(String str) {
						return str.matches("[0-9]*");
					}
					
					/**
					 * @pre - Parameters received
					 * @post - event overridden
					 */
					@Override
					public void insertString(FilterBypass fb, int offset,
							String string, AttributeSet attr)
							throws BadLocationException {
						
						Document doc = fb.getDocument();
						StringBuilder sb = new StringBuilder();
						sb.append(doc.getText(0, doc.getLength()));
						sb.insert(offset, string);
						
						if (this.check(sb.toString())) {
							super.insertString(fb, offset, string, attr);
						}
					}
					
					/**
					 * @pre - Parameters received
					 * @post - event overridden
					 */
					@Override
					public void remove(FilterBypass fb, int offset, int length)
							throws BadLocationException {
						Document doc = fb.getDocument();
						StringBuilder sb = new StringBuilder();
						sb.append(doc.getText(0, doc.getLength()));
						sb.delete(offset, offset + length);
						
						if (this.check(sb.toString())
								|| sb.toString().isEmpty()) {
							super.remove(fb, offset, length);
						}
					}
					
					/**
					 * @pre - Parameters received
					 * @post - event overridden
					 */
					@Override
					public void replace(FilterBypass fb, int offset,
							int length, String text, AttributeSet attrs)
							throws BadLocationException {
						Document doc = fb.getDocument();
						StringBuilder sb = new StringBuilder();
						sb.append(doc.getText(0, doc.getLength()));
						sb.replace(offset, offset + length, text);
						
						if (this.check(sb.toString())) {
							super.replace(fb, offset, length, text, attrs);
						}
					}
				});
	}
}
