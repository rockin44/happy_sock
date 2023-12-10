package com.javadeobfuscator.deobfuscator.ui;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.javadeobfuscator.deobfuscator.ui.component.SynchronousJFXFileChooser;
import com.javadeobfuscator.deobfuscator.ui.util.SwingUtil;
import com.javadeobfuscator.deobfuscator.ui.util.TransformerConfigUtil;
import javafx.stage.FileChooser;

public class TransformerSpecificConfigDialog
{
	private TransformerSpecificConfigDialog()
	{
		throw new UnsupportedOperationException();
	}
	
	public static class TypeMeta {
		public int count;
		public boolean wideWindow;

		public TypeMeta(int count, boolean wideWindow)
		{
			this.count = count;
			this.wideWindow = wideWindow;
		}
	}
	
	public static TypeMeta getTypeMeta(TransformerWithConfig tconfig) {
		int count = 0;
		boolean wideWindow = false;
		Set<Field> fields = TransformerConfigUtil.getTransformerConfigFieldsWithSuperclass(tconfig.getConfig().getClass());

		for (Field field : fields)
		{
			Class<?> t = field.getType();
			if (t.isEnum() || t == boolean.class || t == Boolean.class || t == byte.class
				|| t == Byte.class || t == short.class || t == Short.class || t == int.class || t == Integer.class || t == long.class || t == Long.class
				|| t == float.class || t == Float.class || t == double.class || t == Double.class) {
				count++;
			} else if (t == File.class || t == String.class || t == CharSequence.class) {
				count++;
				wideWindow = true;
			}
		}
		return new TypeMeta(count, wideWindow);
	}

	public static void fill(JDialog jd, TransformerWithConfig tconfig)
	{
		try
		{
			Container root = jd.getContentPane();
			Object config = tconfig.getConfig();
			Set<Field> fields = TransformerConfigUtil.getTransformerConfigFieldsWithSuperclass(config.getClass());
			int gridY = 0;
			for (Field field : fields)
			{
				field.setAccessible(true);
				Class<?> fType = field.getType();
				if (fType == String.class || fType == CharSequence.class)
				{
					JLabel label = new JLabel(field.getName() + ':');
					SwingUtil.registerGBC(root, label, 0, gridY);
					JTextField textField = new JTextField(1);
					SwingUtil.registerGBC(root, textField, 1, gridY, 0, 1);
					label.setLabelFor(textField);
					textField.getDocument().addDocumentListener(new DocumentListener()
					{
						@Override
						public void insertUpdate(DocumentEvent e)
						{
							changedUpdate(e);
						}

						@Override
						public void removeUpdate(DocumentEvent e)
						{
							changedUpdate(e);
						}

						@Override
						public void changedUpdate(DocumentEvent e)
						{
							try
							{
								field.set(config, TransformerConfigUtil.convertToObj(fType, textField.getText()));
							} catch (IllegalAccessException illegalAccessException)
							{
								illegalAccessException.printStackTrace();
							}
						}
					});
				} else if (fType == File.class)
				{
					JLabel label = new JLabel(field.getName());
					SwingUtil.registerGBC(root, label, 0, gridY);

					File currFile = (File) field.get(config);
					String path = currFile == null ? "" : currFile.getAbsolutePath();
					JTextField textField = new JTextField(path);
					SwingUtil.registerGBC(root, textField, 1, gridY, gbc ->
					{
						gbc.weightx = 1;
						gbc.fill = GridBagConstraints.HORIZONTAL;
					});
					label.setLabelFor(textField);
					textField.getDocument().addDocumentListener(new DocumentListener()
					{
						@Override
						public void insertUpdate(DocumentEvent e)
						{
							changedUpdate(e);
						}

						@Override
						public void removeUpdate(DocumentEvent e)
						{
							changedUpdate(e);
						}

						@Override
						public void changedUpdate(DocumentEvent e)
						{
							try
							{
								field.set(config, TransformerConfigUtil.convertToObj(fType, textField.getText()));
							} catch (IllegalAccessException ex)
							{
								ex.printStackTrace();
							}
						}
					});

					JButton fileChooseButton = new JButton("Select");
					SwingUtil.registerGBC(root, fileChooseButton, 2, gridY);
					fileChooseButton.addActionListener(e ->
					{
						SynchronousJFXFileChooser chooser = new SynchronousJFXFileChooser(() ->
						{
							FileChooser ch = new FileChooser();
							ch.setTitle("Select " + field.getName());
							ch.getExtensionFilters().addAll(
									new FileChooser.ExtensionFilter("Jar and Zip files", "*.jar", "*.zip"),
									new FileChooser.ExtensionFilter("Jar files", "*.jar"),
									new FileChooser.ExtensionFilter("Zip files", "*.zip"),
									new FileChooser.ExtensionFilter("Intermediary mapping", "mappings.tiny", "*.tiny"),
									new FileChooser.ExtensionFilter("SRG mapping", "*.srg", "*.tsrg"),
									new FileChooser.ExtensionFilter("CSV mapping", "*.csv"),
									new FileChooser.ExtensionFilter("All Files", "*.*"));
							try
							{
								File prev = (File) field.get(config);
								if (prev != null)
								{
									if (prev.exists())
										ch.setInitialFileName(prev.getName());
									if (prev.getParentFile().exists())
										ch.setInitialDirectory(prev.getParentFile());
								}
							} catch (IllegalAccessException ex)
							{
								ex.printStackTrace();
							}
							return ch;
						});
						File file = chooser.showOpenDialog();
						if (file != null)
						{
							try
							{
								textField.setText(file.getAbsolutePath());
								field.set(config, file);
							} catch (IllegalAccessException ex)
							{
								ex.printStackTrace();
							}
						}
					});
				} else if (fType == boolean.class || fType == Boolean.class)
				{
					JCheckBox checkBox = new JCheckBox(field.getName(), (Boolean) field.get(config));
					SwingUtil.registerGBC(root, checkBox, 0, gridY, 0, 1);
					checkBox.addActionListener(e ->
					{
						try
						{
							field.set(config, checkBox.isSelected());
						} catch (IllegalAccessException ex)
						{
							ex.printStackTrace();
						}
					});
				} else if (fType == byte.class || fType == Byte.class)
				{
					numberInput(root, config, gridY, field, fType, numberValidator(Byte::parseByte));
				} else if (fType == short.class || fType == Short.class)
				{
					numberInput(root, config, gridY, field, fType, numberValidator(Short::parseShort));
				} else if (fType == int.class || fType == Integer.class)
				{
					numberInput(root, config, gridY, field, fType, numberValidator(Integer::parseInt));
				} else if (fType == long.class || fType == Long.class)
				{
					numberInput(root, config, gridY, field, fType, numberValidator(Long::parseLong));
				} else if (fType == float.class || fType == Float.class)
				{
					numberInput(root, config, gridY, field, fType, numberValidator(Float::parseFloat));
				} else if (fType == double.class || fType == Double.class)
				{
					numberInput(root, config, gridY, field, fType, numberValidator(Double::parseDouble));
				} else if (fType.isEnum())
				{
					JLabel label = new JLabel(field.getName() + ':');
					SwingUtil.registerGBC(root, label, 0, gridY);
					JComboBox<?> comboBox = new JComboBox<>(fType.getEnumConstants());
					SwingUtil.registerGBC(root, comboBox, 1, gridY, 0, 1);
					label.setLabelFor(comboBox);
					comboBox.setEditable(false);
					comboBox.setSelectedItem(field.get(config));
					comboBox.addActionListener(e -> {
						try
						{
							field.set(config, comboBox.getSelectedItem());
						} catch (IllegalAccessException ex)
						{
							ex.printStackTrace();
						}
					});
				} else {
					System.out.println("Unknown config field type " + fType + " " + field.getName() + " in transformer " + tconfig.getShortName());
				}
				++gridY;
			}
		} catch (IllegalAccessException ex)
		{
			ex.printStackTrace();
		}
	}

	public interface ThrowableFunction<I, R, T extends Throwable>
	{
		R apply(I i) throws T;
	}

	private static Predicate<String> numberValidator(ThrowableFunction<String, Number, NumberFormatException> fun)
	{
		return s ->
		{
			try
			{
				fun.apply(s);
				return true;
			} catch (NumberFormatException t)
			{
				return false;
			}
		};
	}

	private static void numberInput(Container root, Object config, int gridY, Field field, Class<?> fType, Predicate<String> verifier)
	{
		JLabel label = new JLabel(field.getName());
		SwingUtil.registerGBC(root, label, 0, gridY);

		JTextField textField = new JTextField(1);
		SwingUtil.registerGBC(root, textField, 1, gridY, 0, 1);
		textField.setInputVerifier(new InputVerifier()
		{
			@Override
			public boolean verify(JComponent input)
			{
				return verifier.test(textField.getText());
			}
		});
		label.setLabelFor(textField);
		textField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changedUpdate(e);
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changedUpdate(e);
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				String text = textField.getText();
				if (!verifier.test(text))
				{
					return;
				}
				try
				{
					field.set(config, TransformerConfigUtil.convertToObj(fType, text));
				} catch (IllegalAccessException ex)
				{
					ex.printStackTrace();
				}
			}
		});
	}
}
