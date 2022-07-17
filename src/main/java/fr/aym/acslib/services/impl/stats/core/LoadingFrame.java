package fr.aym.acslib.services.impl.stats.core;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class LoadingFrame extends JFrame {
    /**
     * Displayed text
     */
    private final JLabel mark;
    /**
     * Displayed progress bar
     */
    private final JProgressBar indet = new JProgressBar(0, 100);
    /**
     * Displayed status
     */
    public JLabel status = new JLabel("Préparation...");

    public LoadingFrame(String name, Component parent) {
        this.setTitle(name);
        this.setSize(300, 100);
        this.setResizable(false);
        this.setLocationRelativeTo(parent);
        //this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //if(DBootstrap.getFrameType()==1)
        {
            mark = new JLabel("Le jeu a crashé, envoi du crash au support");
            mark.setFont(new Font("Courrier", Font.TYPE1_FONT, 12));
            mark.setForeground(Color.BLACK);

            indet.setIndeterminate(true);

            Box box = Box.createVerticalBox();

            box.add(mark);
            box.add(indet);
            box.add(status);

            this.getContentPane().add(box);
        }
	    /*else if(DBootstrap.getFrameType()==2)
	    {
		    Container content;
		    this.setBackground(new Color(Color.TRANSLUCENT));
		    this.setUndecorated(true);

		    setContentPane(content = new JPanel()
		    		{
		    	@Override
		    	protected void paintComponent(Graphics g) {
		    		super.paintComponent(g);
		    	    g.drawImage(background, 0, 0, 300, 169, this);
		    	}
		    		});
		    content.setBackground(new Color(Color.TRANSLUCENT));
		    
		    this.setLayout(null);

		    indet.setOpaque(false);
		    indet.setBorder(null);
		    indet.setBorderPainted(false);
		    indet.setForeground(new Color((float)224/255, (float)224/255, (float)224/255, 0.68F));
		    
		    status.setHorizontalAlignment(SwingConstants.CENTER);
		    status.setForeground(Color.WHITE);
		    
			content.add(indet);
		    content.add(status);

		    indet.setBounds(0, 144, 300, 25);
		    status.setBounds(new Rectangle(0, 144, 300, 25));
		    content.setBounds(0, 0, 300, 169);
	    }*/
		/*new Thread() 
		{
			@Override
			public void run() 
			{*/
			   /* Image icon;
				try {
					if(DBootstrap.getFrameType()==700) //PROGRAM EXIT
						return;
					if(DBootstrap.getFrameType()==2)
						background = ImageIO.read(LoadingFrame.class.getResource("/splash.png"));
					icon = ImageIO.read(LoadingFrame.class.getResource("/icon.png"));
				    LoadingFrame.this.setIconImage(icon);
				} catch (IOException e) {
			    	System.out.println("Error during loading icon");
				}*/
        //if(DBootstrap.getFrameType()==1)
        {
				   /* try
				    {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					}
				    catch (Exception e)
				    {
				    	System.out.println("Error during set look and feel");
				    	e.printStackTrace();
					}
				    SwingUtilities.updateComponentTreeUI(LoadingFrame.this);*/
        }
        LoadingFrame.this.setVisible(true);
			/*}
		}.start();*/
	    
	    /*this.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {}
			
			@Override
			public void mousePressed(MouseEvent arg0) {}
			
			@Override
			public void mouseExited(MouseEvent arg0) {}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(!DBootstrap.shouldExit)
					System.exit(0);
			}
		});*/
    }

    public void setProgressIndeterminated(boolean d) {
        this.indet.setIndeterminate(d);
    }

    public void setProgressValue(int value) {
        this.indet.setValue(value);
    }
}
