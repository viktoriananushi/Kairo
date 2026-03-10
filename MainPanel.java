package ui;

import model.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/**
 * Main UI panel with Notion-inspired modern design
 * Features a collapsible sidebar, task summaries, and clean content area
 */
public class MainPanel extends JPanel {

    // Data
    private final Workspace workspace;
    private final TaskSummaryService summaryService;
    private Page currentPage;
    
    // UI Components
    private DefaultListModel<Page> pageListModel;
    private JList<Page> pageList;
    private JPanel contentArea;
    private JPanel sidebarContent;
    private CardLayout contentCardLayout;
    private JPanel cardsContainer;
    
    // View states
    private enum ViewMode { PAGE, UPCOMING, OVERDUE, PRIORITY }
    private ViewMode currentView = ViewMode.PAGE;

    // ========== COLOR PALETTE (Notion-inspired neutral tones) ==========
    private static final Color BACKGROUND = new Color(251, 251, 250);           // Warm white
    private static final Color SIDEBAR_BG = new Color(247, 247, 245);           // Light warm gray
    private static final Color SIDEBAR_HOVER = new Color(237, 237, 235);        // Subtle hover
    private static final Color SIDEBAR_SELECTED = new Color(227, 227, 225);     // Selected state
    private static final Color BORDER_COLOR = new Color(235, 235, 233);         // Soft borders
    private static final Color TEXT_PRIMARY = new Color(55, 53, 47);            // Dark brown-gray
    private static final Color TEXT_SECONDARY = new Color(120, 119, 116);       // Muted text
    private static final Color TEXT_TERTIARY = new Color(155, 154, 151);        // Very muted
    private static final Color ACCENT = new Color(35, 131, 226);                // Blue accent
    private static final Color ACCENT_HOVER = new Color(28, 110, 190);          // Darker blue
    private static final Color SUCCESS = new Color(15, 123, 108);               // Teal green
    private static final Color WARNING = new Color(203, 145, 47);               // Amber
    private static final Color DANGER = new Color(212, 76, 71);                 // Soft red
    private static final Color CARD_BG = Color.WHITE;
    private static final Color OVERDUE_BG = new Color(253, 245, 242);           // Soft red tint
    private static final Color COMPLETED_BG = new Color(241, 250, 247);         // Soft green tint

    // ========== FONTS ==========
    private static final Font FONT_TITLE = new Font("Inter", Font.BOLD, 28);
    private static final Font FONT_HEADING = new Font("Inter", Font.BOLD, 14);
    private static final Font FONT_BODY = new Font("Inter", Font.PLAIN, 14);
    private static final Font FONT_SMALL = new Font("Inter", Font.PLAIN, 12);
    private static final Font FONT_TINY = new Font("Inter", Font.PLAIN, 11);

    public MainPanel(Workspace workspace) {
        this.workspace = workspace;
        this.summaryService = new TaskSummaryService(workspace);
        
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        setupSidebar();
        setupContentArea();

        // Select first page or show welcome
        if (!workspace.getPages().isEmpty()) {
            selectPage(workspace.getPages().get(0));
        } else {
            showWelcome();
        }
    }

    // ==================== SIDEBAR ====================

    private void setupSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_COLOR));

        // Logo/Brand header
        JPanel header = createSidebarHeader();
        sidebar.add(header, BorderLayout.NORTH);

        // Scrollable content
        sidebarContent = new JPanel();
        sidebarContent.setLayout(new BoxLayout(sidebarContent, BoxLayout.Y_AXIS));
        sidebarContent.setBackground(SIDEBAR_BG);
        sidebarContent.setBorder(new EmptyBorder(8, 12, 12, 12));

        // Task Summaries Section
        addSidebarSection("VIEWS", sidebarContent);
        addSummaryButton("Upcoming", "Tasks due soon", ViewMode.UPCOMING, sidebarContent);
        addSummaryButton("Overdue", "Past due date", ViewMode.OVERDUE, sidebarContent);
        addSummaryButton("Priority", "By importance", ViewMode.PRIORITY, sidebarContent);

        sidebarContent.add(Box.createVerticalStrut(16));

        // Pages Section
        addSidebarSection("PAGES", sidebarContent);
        
        // Page list
        pageListModel = new DefaultListModel<>();
        workspace.getPages().forEach(pageListModel::addElement);
        
        pageList = new JList<>(pageListModel);
        pageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pageList.setBackground(SIDEBAR_BG);
        pageList.setFixedCellHeight(36);
        pageList.setBorder(null);
        pageList.setCellRenderer(new PageListCellRenderer());
        
        pageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Page selected = pageList.getSelectedValue();
                if (selected != null) {
                    currentView = ViewMode.PAGE;
                    selectPage(selected);
                }
            }
        });

        // Wrap in a panel to control sizing
        JPanel pageListWrapper = new JPanel(new BorderLayout());
        pageListWrapper.setBackground(SIDEBAR_BG);
        pageListWrapper.add(pageList, BorderLayout.NORTH);
        sidebarContent.add(pageListWrapper);

        JScrollPane scrollPane = new JScrollPane(sidebarContent);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(SIDEBAR_BG);
        scrollPane.getViewport().setBackground(SIDEBAR_BG);
        sidebar.add(scrollPane, BorderLayout.CENTER);

        // New Page button at bottom
        JPanel bottomPanel = createNewPageButton();
        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);
    }

    private JPanel createSidebarHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SIDEBAR_BG);
        header.setBorder(new EmptyBorder(20, 20, 16, 20));

        JLabel logo = new JLabel("Kairo");
        logo.setFont(new Font("Inter", Font.BOLD, 20));
        logo.setForeground(TEXT_PRIMARY);

        JLabel tagline = new JLabel("Smart Task System");
        tagline.setFont(FONT_TINY);
        tagline.setForeground(TEXT_TERTIARY);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(SIDEBAR_BG);
        textPanel.add(logo);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(tagline);

        header.add(textPanel, BorderLayout.WEST);
        return header;
    }

    private void addSidebarSection(String title, JPanel parent) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("Inter", Font.BOLD, 11));
        label.setForeground(TEXT_TERTIARY);
        label.setBorder(new EmptyBorder(8, 8, 8, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(label);
    }

    private void addSummaryButton(String title, String subtitle, ViewMode mode, JPanel parent) {
        JPanel btn = new JPanel(new BorderLayout());
        btn.setBackground(SIDEBAR_BG);
        btn.setBorder(new EmptyBorder(8, 12, 8, 12));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_BODY);
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(FONT_TINY);
        subtitleLabel.setForeground(TEXT_TERTIARY);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        btn.add(textPanel, BorderLayout.CENTER);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(SIDEBAR_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(currentView == mode ? SIDEBAR_SELECTED : SIDEBAR_BG);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                currentView = mode;
                pageList.clearSelection();
                showSummaryView(mode);
                btn.setBackground(SIDEBAR_SELECTED);
            }
        });

        parent.add(btn);
    }

    private JPanel createNewPageButton() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SIDEBAR_BG);
        panel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
            new EmptyBorder(12, 16, 12, 16)
        ));

        JButton newPageBtn = createStyledButton("+ New Page", ACCENT, Color.WHITE);
        newPageBtn.addActionListener(e -> createNewPage());
        
        panel.add(newPageBtn, BorderLayout.CENTER);
        return panel;
    }

    // ==================== CONTENT AREA ====================

    private void setupContentArea() {
        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(BACKGROUND);
        add(contentArea, BorderLayout.CENTER);
    }

    private void showWelcome() {
        contentArea.removeAll();
        
        JPanel welcome = new JPanel();
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        welcome.setBackground(BACKGROUND);
        welcome.setBorder(new EmptyBorder(100, 60, 60, 60));

        JLabel emoji = new JLabel("Welcome to Kairo");
        emoji.setFont(FONT_TITLE);
        emoji.setForeground(TEXT_PRIMARY);
        emoji.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Your smart task and scheduling system");
        subtitle.setFont(new Font("Inter", Font.PLAIN, 16));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Create a new page to get started, or select a view from the sidebar.");
        hint.setFont(FONT_BODY);
        hint.setForeground(TEXT_TERTIARY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        welcome.add(emoji);
        welcome.add(Box.createVerticalStrut(8));
        welcome.add(subtitle);
        welcome.add(Box.createVerticalStrut(24));
        welcome.add(hint);
        welcome.add(Box.createVerticalStrut(32));

        JButton createBtn = createStyledButton("Create your first page", ACCENT, Color.WHITE);
        createBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createBtn.addActionListener(e -> createNewPage());
        welcome.add(createBtn);

        contentArea.add(welcome, BorderLayout.NORTH);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private void selectPage(Page page) {
        this.currentPage = page;
        workspace.selectPage(page.getId());
        showPageContent(page);
    }

    private void showPageContent(Page page) {
        contentArea.removeAll();
        
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(BACKGROUND);
        mainContent.setBorder(new EmptyBorder(32, 48, 32, 48));

        // Header with page name and actions
        JPanel header = createPageHeader(page);
        mainContent.add(header, BorderLayout.NORTH);

        // Content sections (Tasks and Notes)
        JPanel sections = new JPanel();
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setBackground(BACKGROUND);

        // Tasks section
        JPanel tasksSection = createTasksSection(page);
        sections.add(tasksSection);
        
        sections.add(Box.createVerticalStrut(32));

        // Notes section
        JPanel notesSection = createNotesSection(page);
        sections.add(notesSection);

        JScrollPane scrollPane = new JScrollPane(sections);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BACKGROUND);
        scrollPane.getViewport().setBackground(BACKGROUND);
        
        mainContent.add(scrollPane, BorderLayout.CENTER);
        contentArea.add(mainContent, BorderLayout.CENTER);
        
        contentArea.revalidate();
        contentArea.repaint();
    }

    private JPanel createPageHeader(Page page) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND);
        header.setBorder(new EmptyBorder(0, 0, 24, 0));

        // Page title (editable on click)
        JLabel titleLabel = new JLabel(page.getName());
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        titleLabel.setToolTipText("Click to rename");
        
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String newName = showInputDialog("Rename Page", "Enter new name:", page.getName());
                if (newName != null && !newName.trim().isEmpty()) {
                    page.rename(newName.trim());
                    titleLabel.setText(newName.trim());
                    pageList.repaint();
                }
            }
        });

        // Actions panel
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(BACKGROUND);

        JButton deleteBtn = createTextButton("Delete Page", DANGER);
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete \"" + page.getName() + "\"?",
                "Delete Page",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                workspace.deletePage(page.getId());
                pageListModel.removeElement(page);
                if (!pageListModel.isEmpty()) {
                    selectPage(pageListModel.get(0));
                } else {
                    showWelcome();
                }
            }
        });
        actions.add(deleteBtn);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    private JPanel createTasksSection(Page page) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(BACKGROUND);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section header
        JPanel sectionHeader = new JPanel(new BorderLayout());
        sectionHeader.setBackground(BACKGROUND);
        sectionHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel titleLabel = new JLabel("Tasks");
        titleLabel.setFont(FONT_HEADING);
        titleLabel.setForeground(TEXT_PRIMARY);

        JButton addBtn = createTextButton("+ Add Task", ACCENT);
        addBtn.addActionListener(e -> showAddTaskDialog(page));

        sectionHeader.add(titleLabel, BorderLayout.WEST);
        sectionHeader.add(addBtn, BorderLayout.EAST);
        section.add(sectionHeader);
        section.add(Box.createVerticalStrut(12));

        // Task cards
        List<Task> tasks = page.getTasks();
        if (tasks.isEmpty()) {
            JLabel emptyLabel = new JLabel("No tasks yet. Click \"+ Add Task\" to create one.");
            emptyLabel.setFont(FONT_BODY);
            emptyLabel.setForeground(TEXT_TERTIARY);
            section.add(emptyLabel);
        } else {
            tasks.stream()
                .sorted(Comparator.comparing(Task::getStatus)
                    .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(task -> {
                    JPanel card = createTaskCard(task, page, true);
                    section.add(card);
                    section.add(Box.createVerticalStrut(8));
                });
        }

        return section;
    }

    private JPanel createTaskCard(Task task, Page page, boolean showActions) {
        LocalDate today = LocalDate.now();
        boolean isOverdue = task.isOverdue(today);
        boolean isCompleted = task.isCompleted();

        JPanel card = new RoundedPanel(12);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        if (isCompleted) {
            card.setBackground(COMPLETED_BG);
        } else if (isOverdue) {
            card.setBackground(OVERDUE_BG);
        } else {
            card.setBackground(CARD_BG);
        }

        // Left side: checkbox and title
        JPanel leftPanel = new JPanel(new BorderLayout(12, 0));
        leftPanel.setOpaque(false);

        JCheckBox checkbox = new JCheckBox();
        checkbox.setSelected(isCompleted);
        checkbox.setOpaque(false);
        checkbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkbox.addActionListener(e -> {
            if (checkbox.isSelected()) {
                task.markComplete();
            } else {
                task.setStatus(Status.NOT_STARTED);
            }
            refreshCurrentView();
        });
        leftPanel.add(checkbox, BorderLayout.WEST);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel(task.getTitle());
        titleLabel.setFont(FONT_BODY);
        titleLabel.setForeground(isCompleted ? TEXT_TERTIARY : TEXT_PRIMARY);
        if (isCompleted) {
            titleLabel.setText("<html><s>" + task.getTitle() + "</s></html>");
        }
        titlePanel.add(titleLabel);

        // Due date and priority info
        StringBuilder infoText = new StringBuilder();
        if (task.getDueDate() != null) {
            String dateStr = task.getDueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
            if (isOverdue) {
                infoText.append("<font color='#D44C47'>").append(dateStr).append(" (Overdue)</font>");
            } else if (task.getDueDate().equals(today)) {
                infoText.append("<font color='#CB912F'>").append(dateStr).append(" (Today)</font>");
            } else {
                infoText.append(dateStr);
            }
        }
        if (task.getPriority() != null) {
            if (infoText.length() > 0) infoText.append("  ·  ");
            Color priorityColor = getPriorityColor(task.getPriority());
            infoText.append("<font color='").append(String.format("#%02x%02x%02x", 
                priorityColor.getRed(), priorityColor.getGreen(), priorityColor.getBlue()))
                .append("'>").append(task.getPriority()).append(" priority</font>");
        }
        
        if (infoText.length() > 0) {
            JLabel infoLabel = new JLabel("<html>" + infoText + "</html>");
            infoLabel.setFont(FONT_SMALL);
            infoLabel.setForeground(TEXT_SECONDARY);
            titlePanel.add(Box.createVerticalStrut(4));
            titlePanel.add(infoLabel);
        }

        leftPanel.add(titlePanel, BorderLayout.CENTER);
        card.add(leftPanel, BorderLayout.CENTER);

        // Right side: status badge and actions
        if (showActions) {
            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            rightPanel.setOpaque(false);

            // Status badge
            JLabel statusBadge = createStatusBadge(task.getStatus());
            rightPanel.add(statusBadge);

            // Edit button
            JButton editBtn = createIconButton("Edit");
            editBtn.addActionListener(e -> showEditTaskDialog(task, page));
            rightPanel.add(editBtn);

            // Delete button
            JButton deleteBtn = createIconButton("Delete");
            deleteBtn.setForeground(DANGER);
            deleteBtn.addActionListener(e -> {
                page.removeTask(task.getId());
                refreshCurrentView();
            });
            rightPanel.add(deleteBtn);

            card.add(rightPanel, BorderLayout.EAST);
        }

        return card;
    }

    private JPanel createNotesSection(Page page) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(BACKGROUND);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Section header
        JPanel sectionHeader = new JPanel(new BorderLayout());
        sectionHeader.setBackground(BACKGROUND);
        sectionHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel titleLabel = new JLabel("Notes");
        titleLabel.setFont(FONT_HEADING);
        titleLabel.setForeground(TEXT_PRIMARY);

        JButton addBtn = createTextButton("+ Add Note", ACCENT);
        addBtn.addActionListener(e -> showAddNoteDialog(page));

        sectionHeader.add(titleLabel, BorderLayout.WEST);
        sectionHeader.add(addBtn, BorderLayout.EAST);
        section.add(sectionHeader);
        section.add(Box.createVerticalStrut(12));

        // Note cards
        List<Note> notes = page.getNotes();
        if (notes.isEmpty()) {
            JLabel emptyLabel = new JLabel("No notes yet. Click \"+ Add Note\" to create one.");
            emptyLabel.setFont(FONT_BODY);
            emptyLabel.setForeground(TEXT_TERTIARY);
            section.add(emptyLabel);
        } else {
            for (Note note : notes) {
                JPanel card = createNoteCard(note, page);
                section.add(card);
                section.add(Box.createVerticalStrut(8));
            }
        }

        return section;
    }

    private JPanel createNoteCard(Note note, Page page) {
        JPanel card = new RoundedPanel(12);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setBackground(CARD_BG);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Note content
        JTextArea contentArea = new JTextArea(note.getContent());
        contentArea.setFont(FONT_BODY);
        contentArea.setForeground(TEXT_PRIMARY);
        contentArea.setBackground(CARD_BG);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setBorder(null);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(contentArea, BorderLayout.CENTER);

        // Created date
        JLabel dateLabel = new JLabel(note.getCreatedAt().toString().substring(0, 10));
        dateLabel.setFont(FONT_TINY);
        dateLabel.setForeground(TEXT_TERTIARY);
        leftPanel.add(dateLabel, BorderLayout.SOUTH);

        card.add(leftPanel, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);

        JButton editBtn = createIconButton("Edit");
        editBtn.addActionListener(e -> showEditNoteDialog(note, page));
        actions.add(editBtn);

        JButton deleteBtn = createIconButton("Delete");
        deleteBtn.setForeground(DANGER);
        deleteBtn.addActionListener(e -> {
            page.removeNote(note.getId());
            refreshCurrentView();
        });
        actions.add(deleteBtn);

        card.add(actions, BorderLayout.EAST);

        return card;
    }

    // ==================== SUMMARY VIEWS ====================

    private void showSummaryView(ViewMode mode) {
        contentArea.removeAll();
        
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(BACKGROUND);
        mainContent.setBorder(new EmptyBorder(32, 48, 32, 48));

        String title;
        String subtitle;
        List<Task> tasks;
        LocalDate today = LocalDate.now();

        switch (mode) {
            case UPCOMING:
                title = "Upcoming Tasks";
                subtitle = "Tasks with future due dates";
                tasks = summaryService.getUpcoming(today);
                break;
            case OVERDUE:
                title = "Overdue Tasks";
                subtitle = "Tasks past their due date";
                tasks = summaryService.getOverdue(today);
                break;
            case PRIORITY:
                title = "Tasks by Priority";
                subtitle = "Organized by importance level";
                tasks = summaryService.getPriority();
                break;
            default:
                return;
        }

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(BACKGROUND);
        header.setBorder(new EmptyBorder(0, 0, 24, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(FONT_BODY);
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);

        mainContent.add(header, BorderLayout.NORTH);

        // Task list
        JPanel taskList = new JPanel();
        taskList.setLayout(new BoxLayout(taskList, BoxLayout.Y_AXIS));
        taskList.setBackground(BACKGROUND);

        if (tasks.isEmpty()) {
            JLabel emptyLabel = new JLabel("No tasks to display.");
            emptyLabel.setFont(FONT_BODY);
            emptyLabel.setForeground(TEXT_TERTIARY);
            emptyLabel.setBorder(new EmptyBorder(20, 0, 0, 0));
            taskList.add(emptyLabel);
        } else {
            for (Task task : tasks) {
                // Find the page this task belongs to
                Page taskPage = findPageForTask(task);
                JPanel card = createTaskCard(task, taskPage, true);
                
                // Add page name indicator
                if (taskPage != null) {
                    JLabel pageLabel = new JLabel("in " + taskPage.getName());
                    pageLabel.setFont(FONT_TINY);
                    pageLabel.setForeground(TEXT_TERTIARY);
                    pageLabel.setBorder(new EmptyBorder(0, 44, 4, 0));
                    taskList.add(pageLabel);
                }
                
                taskList.add(card);
                taskList.add(Box.createVerticalStrut(12));
            }
        }

        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BACKGROUND);
        scrollPane.getViewport().setBackground(BACKGROUND);

        mainContent.add(scrollPane, BorderLayout.CENTER);
        contentArea.add(mainContent, BorderLayout.CENTER);
        
        contentArea.revalidate();
        contentArea.repaint();
    }

    private Page findPageForTask(Task task) {
        for (Page page : workspace.getPages()) {
            for (Task t : page.getTasks()) {
                if (t.getId().equals(task.getId())) {
                    return page;
                }
            }
        }
        return null;
    }

    // ==================== DIALOGS ====================

    private void createNewPage() {
        String name = showInputDialog("New Page", "Enter page name:", "");
        if (name != null && !name.trim().isEmpty()) {
            Page p = workspace.createPage(name.trim());
            pageListModel.addElement(p);
            pageList.setSelectedValue(p, true);
            currentView = ViewMode.PAGE;
            selectPage(p);
        }
    }

    private void showAddTaskDialog(Page page) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title field
        JTextField titleField = new JTextField(20);
        addFormField(panel, "Title *", titleField);

        // Due date field
        JTextField dueDateField = new JTextField(20);
        dueDateField.setToolTipText("Format: YYYY-MM-DD");
        addFormField(panel, "Due Date (YYYY-MM-DD)", dueDateField);

        // Priority dropdown
        JComboBox<Priority> priorityCombo = new JComboBox<>(Priority.values());
        priorityCombo.setSelectedItem(Priority.MEDIUM);
        addFormField(panel, "Priority", priorityCombo);

        // Status dropdown
        JComboBox<Status> statusCombo = new JComboBox<>(Status.values());
        statusCombo.setSelectedItem(Status.NOT_STARTED);
        addFormField(panel, "Status", statusCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Task", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Task title is required.", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDate dueDate = null;
            String dueDateStr = dueDateField.getText().trim();
            if (!dueDateStr.isEmpty()) {
                try {
                    dueDate = LocalDate.parse(dueDateStr);
                } catch (DateTimeParseException e) {
                    JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", 
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            Task newTask = new Task(title, dueDate, 
                (Priority) priorityCombo.getSelectedItem(),
                (Status) statusCombo.getSelectedItem());
            page.addTask(newTask);
            refreshCurrentView();
        }
    }

    private void showEditTaskDialog(Task task, Page page) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title field
        JTextField titleField = new JTextField(task.getTitle(), 20);
        addFormField(panel, "Title *", titleField);

        // Due date field
        JTextField dueDateField = new JTextField(
            task.getDueDate() != null ? task.getDueDate().toString() : "", 20);
        dueDateField.setToolTipText("Format: YYYY-MM-DD");
        addFormField(panel, "Due Date (YYYY-MM-DD)", dueDateField);

        // Priority dropdown
        JComboBox<Priority> priorityCombo = new JComboBox<>(Priority.values());
        priorityCombo.setSelectedItem(task.getPriority());
        addFormField(panel, "Priority", priorityCombo);

        // Status dropdown
        JComboBox<Status> statusCombo = new JComboBox<>(Status.values());
        statusCombo.setSelectedItem(task.getStatus());
        addFormField(panel, "Status", statusCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Task", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Task title is required.", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDate dueDate = null;
            String dueDateStr = dueDateField.getText().trim();
            if (!dueDateStr.isEmpty()) {
                try {
                    dueDate = LocalDate.parse(dueDateStr);
                } catch (DateTimeParseException e) {
                    JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", 
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            task.setTitle(title);
            task.setDueDate(dueDate);
            task.setPriority((Priority) priorityCombo.getSelectedItem());
            task.setStatus((Status) statusCombo.getSelectedItem());
            refreshCurrentView();
        }
    }

    private void showAddNoteDialog(Page page) {
        JTextArea textArea = new JTextArea(5, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);

        int result = JOptionPane.showConfirmDialog(this, scrollPane, "Add Note", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String content = textArea.getText().trim();
            if (!content.isEmpty()) {
                Note newNote = new Note(content);
                page.addNote(newNote);
                refreshCurrentView();
            }
        }
    }

    private void showEditNoteDialog(Note note, Page page) {
        JTextArea textArea = new JTextArea(note.getContent(), 5, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);

        int result = JOptionPane.showConfirmDialog(this, scrollPane, "Edit Note", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String content = textArea.getText().trim();
            if (!content.isEmpty()) {
                note.setContent(content);
                refreshCurrentView();
            }
        }
    }

    private void addFormField(JPanel panel, String label, JComponent field) {
        JPanel fieldPanel = new JPanel(new BorderLayout(0, 4));
        fieldPanel.setOpaque(false);
        fieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        fieldPanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(FONT_SMALL);
        fieldLabel.setForeground(TEXT_SECONDARY);

        fieldPanel.add(fieldLabel, BorderLayout.NORTH);
        fieldPanel.add(field, BorderLayout.CENTER);

        panel.add(fieldPanel);
    }

    private String showInputDialog(String title, String message, String initialValue) {
        return JOptionPane.showInputDialog(this, message, title, 
            JOptionPane.PLAIN_MESSAGE);
    }

    // ==================== HELPER METHODS ====================

    private void refreshCurrentView() {
        switch (currentView) {
            case PAGE:
                if (currentPage != null) {
                    showPageContent(currentPage);
                }
                break;
            case UPCOMING:
            case OVERDUE:
            case PRIORITY:
                showSummaryView(currentView);
                break;
        }
    }

    private Color getPriorityColor(Priority priority) {
        switch (priority) {
            case HIGH: return DANGER;
            case MEDIUM: return WARNING;
            case LOW: return SUCCESS;
            default: return TEXT_SECONDARY;
        }
    }

    private JLabel createStatusBadge(Status status) {
        JLabel badge = new JLabel(status.toString());
        badge.setFont(FONT_TINY);
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(4, 8, 4, 8));
        
        switch (status) {
            case COMPLETED:
                badge.setBackground(new Color(232, 245, 233));
                badge.setForeground(SUCCESS);
                break;
            case IN_PROGRESS:
                badge.setBackground(new Color(227, 242, 253));
                badge.setForeground(ACCENT);
                break;
            case NOT_STARTED:
                badge.setBackground(new Color(245, 245, 245));
                badge.setForeground(TEXT_SECONDARY);
                break;
            case CANCELLED:
                badge.setBackground(new Color(245, 245, 245));
                badge.setForeground(TEXT_TERTIARY);
                break;
        }
        
        return badge;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bg.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bg);
            }
        });
        
        return button;
    }

    private JButton createTextButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY);
        button.setForeground(color);
        button.setBackground(BACKGROUND);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(color.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(color);
            }
        });
        
        return button;
    }

    private JButton createIconButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_TINY);
        button.setForeground(TEXT_TERTIARY);
        button.setBackground(CARD_BG);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(TEXT_PRIMARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(TEXT_TERTIARY);
            }
        });
        
        return button;
    }

    // ==================== CUSTOM COMPONENTS ====================

    /**
     * Custom JList cell renderer for pages with modern styling
     */
    private class PageListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            Page page = (Page) value;
            
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(isSelected ? SIDEBAR_SELECTED : SIDEBAR_BG);
            panel.setBorder(new EmptyBorder(8, 12, 8, 12));
            
            JLabel nameLabel = new JLabel(page.getName());
            nameLabel.setFont(FONT_BODY);
            nameLabel.setForeground(TEXT_PRIMARY);
            
            JLabel countLabel = new JLabel(page.getTasks().size() + " tasks");
            countLabel.setFont(FONT_TINY);
            countLabel.setForeground(TEXT_TERTIARY);
            
            panel.add(nameLabel, BorderLayout.CENTER);
            panel.add(countLabel, BorderLayout.EAST);
            
            return panel;
        }
    }

    /**
     * Custom JPanel with rounded corners
     */
    private static class RoundedPanel extends JPanel {
        private final int radius;

        public RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(235, 235, 233));
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            g2.dispose();
        }
    }
}
