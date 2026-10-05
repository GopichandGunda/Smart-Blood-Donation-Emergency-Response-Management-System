package com.bloodmanagement.ui;

import com.bloodmanagement.enums.BloodGroup;
import com.bloodmanagement.enums.BloodUnitStatus;
import com.bloodmanagement.enums.DonorStatus;
import com.bloodmanagement.enums.EmergencyLevel;
import com.bloodmanagement.enums.RequestStatus;
import com.bloodmanagement.enums.TransferStatus;
import com.bloodmanagement.enums.UserRole;
import com.bloodmanagement.model.AuditLog;
import com.bloodmanagement.model.BloodBank;
import com.bloodmanagement.model.BloodTransfer;
import com.bloodmanagement.model.BloodUnit;
import com.bloodmanagement.model.Donor;
import com.bloodmanagement.model.EmergencyRequest;
import com.bloodmanagement.model.Hospital;
import com.bloodmanagement.model.User;
import com.bloodmanagement.service.AppServices;
import com.bloodmanagement.service.DonorService;
import com.bloodmanagement.service.DonorMatchingService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

public final class DashboardFrame extends JFrame {
    private static final Color RED = new Color(171, 38, 54);
    private static final Color NAVY = new Color(29, 43, 59);
    private static final Color LIGHT = new Color(246, 248, 250);
    private final User user;
    private final AppServices services;
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<Component, Runnable> refreshActions = new LinkedHashMap<>();

    public DashboardFrame(User user, AppServices services) {
        super("Emergency Blood Management | Workspace");
        this.user = user;
        this.services = services;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        getContentPane().setBackground(LIGHT);
        setLayout(new BorderLayout());
        setJMenuBar(null);
        add(createHeader(), BorderLayout.NORTH);
        tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        tabs.setBackground(Color.WHITE);
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        buildTabs();
        tabs.addChangeListener(event -> refreshCurrent());
        add(tabs, BorderLayout.CENTER);
        refreshCurrent();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 22));
        JPanel branding = new JPanel(new BorderLayout(14, 0));
        branding.setOpaque(false);
        JLabel logo = new JLabel("LIFE / LINK");
        logo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        logo.setForeground(new Color(255, 190, 195));
        branding.add(logo, BorderLayout.WEST);
        JLabel title = new JLabel("Emergency Blood Management");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        branding.add(title, BorderLayout.CENTER);
        header.add(branding, BorderLayout.WEST);

        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        account.setOpaque(false);
        JLabel identity = new JLabel(user.username() + "  ·  " + roleLabel(user.role()));
        identity.setForeground(new Color(226, 232, 238));
        identity.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        account.add(identity);
        JButton logout = new JButton("Sign out");
        logout.setFocusPainted(false);
        logout.addActionListener(event -> {
            services.emergencyProcessing.close();
            dispose();
            new LoginFrame().setVisible(true);
        });
        account.add(logout);
        header.add(account, BorderLayout.EAST);
        return header;
    }

    private void buildTabs() {
        addTab("Overview", "Overview", this::homePage);
        if (user.role() == UserRole.DONOR) {
            addTab("My profile", "My donor profile", this::donorProfilePage);
            addTab("Donation offers", "Emergency donation offers", this::donorOffersPage);
            addTab("Notifications", "My notifications", this::notificationsPage);
            return;
        }
        addTab("Donors", "Donor directory", this::donorsPage);
        addTab("Emergency requests", "Emergency requests", this::requestsPage);
        addTab("Blood inventory", "Blood inventory", this::inventoryPage);
        if (user.role() == UserRole.ADMIN) {
            addTab("Hospitals", "Hospitals", this::hospitalsPage);
            addTab("Blood banks", "Blood banks", this::banksPage);
        }
        if (user.role() == UserRole.BLOOD_BANK_STAFF) {
            addTab("Donations", "Record donation", this::donationsPage);
        }
        addTab("Blood transfers", "Blood transfers", this::transfersPage);
        addTab("Reports", "Reports & analytics", this::reportsPage);
        if (user.role() == UserRole.ADMIN) {
            addTab("Accounts", "User accounts", this::accountsPage);
            addTab("Audit log", "Audit log", this::auditPage);
        }
    }

    private void addTab(String title, String heading, java.util.function.Supplier<JPanel> pageFactory) {
        JPanel page = pageFactory.get();
        tabs.addTab(title, page);
        page.putClientProperty("heading", heading);
        page.putClientProperty("loaded", Boolean.FALSE);
    }

    private JPanel homePage() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(LIGHT);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel welcome = new JLabel("Good day, " + user.username());
        welcome.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        welcome.setForeground(NAVY);
        JLabel sub = new JLabel("Here is the latest operational snapshot.");
        sub.setForeground(new Color(112, 123, 134));
        JPanel titles = new JPanel(new BorderLayout(0, 6));
        titles.setOpaque(false);
        titles.add(welcome, BorderLayout.NORTH);
        titles.add(sub, BorderLayout.SOUTH);
        top.add(titles, BorderLayout.WEST);
        JButton refresh = primaryButton("Refresh");
        refresh.addActionListener(event -> refreshCurrent(true));
        top.add(refresh, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        JPanel cards = new JPanel(new java.awt.GridLayout(0, 3, 12, 12));
        cards.setOpaque(false);
        content.add(cards, BorderLayout.NORTH);
        JPanel alertsPanel = new JPanel(new BorderLayout(10, 10));
        alertsPanel.setBackground(Color.WHITE);
        alertsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 231, 236)),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        JLabel alertsTitle = new JLabel("Blood stock alerts");
        alertsTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        alertsTitle.setForeground(NAVY);
        alertsPanel.add(alertsTitle, BorderLayout.NORTH);
        JLabel alertsBody = new JLabel("Inventory alerts will appear here.");
        alertsBody.setVerticalAlignment(SwingConstants.TOP);
        alertsBody.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        alertsPanel.add(alertsBody, BorderLayout.CENTER);
        content.add(alertsPanel, BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        refreshActions.put(panel, () -> {
            Map<String, Long> summary;
            List<String> alerts;
            if (user.role() == UserRole.DONOR) {
                Donor donor = services.donorService.findForUser(user.id());
                summary = new LinkedHashMap<>();
                summary.put("My donations", (long) donor.totalDonations());
                summary.put("My eligibility", services.donorEligibility
                        .checkEligibility(donor, LocalDate.now()) ? 1L : 0L);
                summary.put("Available to donate", donor.available() ? 1L : 0L);
                summary.put("My active offers", services.donorMatches.findOffers(user.id()).stream()
                        .filter(offer -> offer.response() == null).count());
                alerts = List.of();
            } else {
                summary = services.reports.summary();
                alerts = services.inventory.alerts().stream()
                        .map(alert -> alert.message() + "  ·  " + alert.bloodGroup() + ": "
                                + alert.currentStock() + " units (minimum " + alert.minimumStock() + ")")
                        .toList();
            }
            cards.removeAll();
            int count = 0;
            for (var entry : summary.entrySet()) {
                if (count++ == 6) {
                    break;
                }
                cards.add(statCard(entry.getKey(), entry.getValue().toString()));
            }
            while (cards.getComponentCount() < 6) {
                cards.add(statCard("—", "—"));
            }
            alertsBody.setText(user.role() == UserRole.DONOR
                    ? "<html>View your matching opportunities and notifications in the donor tabs.</html>"
                    : alerts.isEmpty()
                    ? "<html><span style='color:#34835e'>All blood groups are above the configured low-stock threshold.</span></html>"
                    : "<html>" + String.join("<br><br>", alerts) + "</html>");
            cards.revalidate();
            cards.repaint();
        });
        return panel;
    }

    private JPanel donorsPage() {
        JTextField search = new JTextField(18);
        return tablePage(new String[]{"ID", "Name", "Blood group", "City", "Phone", "Eligible", "Available", "Status"},
                () -> services.donorService.search(search.getText()).stream().map(donor -> new Object[]{
                    donor.id(), donor.fullName(), donor.bloodGroup(), donor.city(), donor.phone(),
                    donor.eligible() ? "Yes" : "No", donor.available() ? "Yes" : "No", donor.status()}).toList(),
                List.of(
                        new ToolbarItem("Register donor", true, () -> createDonor()),
                        new ToolbarItem("Search", false, () -> refreshCurrent(true)),
                        new ToolbarItem("Toggle availability", false, () -> toggleDonorAvailability()),
                        new ToolbarItem("Activate / deactivate", false, () -> toggleDonorStatus())),
                search);
    }

    private JPanel requestsPage() {
        return tablePage(new String[]{"ID", "Priority", "Patient", "Hospital ID", "Blood group", "Units",
                    "Required by", "Status"},
                () -> services.emergencyRequests.findAll().stream().map(request -> new Object[]{
                    request.id(), request.emergencyLevel(), request.patientName(), request.hospitalId(),
                    request.bloodGroupRequired(), request.unitsRequired(), request.requiredDate(), request.status()}).toList(),
                List.of(
                        new ToolbarItem("New request", true, () -> createRequest()),
                        new ToolbarItem("Find donors", false, () -> showMatches()),
                        new ToolbarItem("Change status", false, () -> changeRequestStatus()),
                        new ToolbarItem("Reserve selected blood", false, () -> reserveStock())));
    }

    private JPanel inventoryPage() {
        return tablePage(new String[]{"Unit ID", "Blood group", "Quantity", "Collection date", "Expiry date",
                    "Blood bank ID", "Location", "Status"},
                () -> services.inventory.findAll().stream().map(unit -> new Object[]{
                    unit.id(), unit.bloodGroup(), unit.quantity(), unit.collectionDate(), unit.expiryDate(),
                    unit.bloodBankId(), unit.storageLocation(), unit.status()}).toList(),
                List.of(
                        new ToolbarItem("Add blood stock", true, () -> addStock()),
                        new ToolbarItem("Issue reservation", false, () -> issueStock()),
                        new ToolbarItem("Refresh / expire old stock", false, () -> refreshCurrent(true))));
    }

    private JPanel hospitalsPage() {
        return tablePage(new String[]{"ID", "Hospital", "City", "Phone", "Email", "Emergency contact", "Active"},
                () -> services.hospitals.findAll().stream().map(hospital -> new Object[]{
                    hospital.id(), hospital.name(), hospital.city(), hospital.phone(), hospital.email(),
                    hospital.emergencyContact(), hospital.active() ? "Yes" : "No"}).toList(),
                List.of(new ToolbarItem("Add hospital", true, () -> addHospital())));
    }

    private JPanel banksPage() {
        return tablePage(new String[]{"ID", "Blood bank", "City", "Phone", "Email", "Storage capacity", "Stock", "Active"},
                () -> services.bloodBanks.findAll().stream().map(bank -> new Object[]{
                    bank.id(), bank.name(), bank.city(), bank.phone(), bank.email(),
                    bank.storageCapacity(), bank.currentStock(), bank.active() ? "Yes" : "No"}).toList(),
                List.of(new ToolbarItem("Add blood bank", true, () -> addBank())));
    }

    private JPanel donationsPage() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(LIGHT);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 231, 236)),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)));
        JTextField donorId = new JTextField(22);
        JTextField units = new JTextField("1", 22);
        JComboBox<BloodBank> bank = new JComboBox<>();
        JLabel helper = new JLabel("<html>Eligibility and account status are checked before recording.<br>"
                + "Donation record, donor update, inventory addition, and audit event are committed together.</html>");
        helper.setForeground(new Color(96, 108, 120));
        addFormRow(card, 0, "Donor ID", donorId);
        addFormRow(card, 1, "Units", units);
        addFormRow(card, 2, "Blood bank", bank);
        addFormRow(card, 3, "", helper);
        JButton save = primaryButton("Record verified donation");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.insets = new Insets(18, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;
        card.add(save, c);
        save.addActionListener(event -> safely(() -> {
            BloodBank selectedBank = (BloodBank) bank.getSelectedItem();
            if (selectedBank == null) {
                throw new IllegalArgumentException("Register a blood bank before recording a donation.");
            }
            services.donations.record(Long.parseLong(donorId.getText().trim()),
                    Integer.parseInt(units.getText().trim()), selectedBank.id(), user.id());
            JOptionPane.showMessageDialog(this, "Donation recorded, eligibility updated, and inventory replenished.");
            donorId.setText("");
            units.setText("1");
        }));
        panel.add(card, BorderLayout.NORTH);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        refreshActions.put(panel, () -> {
            bank.removeAllItems();
            services.bloodBanks.findAll().stream().filter(BloodBank::active).forEach(bank::addItem);
        });
        return panel;
    }

    private JPanel transfersPage() {
        return tablePage(new String[]{"ID", "Source bank", "Destination bank", "Blood group", "Units",
                    "Requested", "Status", "Staff ID"},
                () -> services.transfers.findAll().stream().map(transfer -> new Object[]{
                    transfer.id(), transfer.sourceBloodBankId(), transfer.destinationBloodBankId(),
                    transfer.bloodGroup(), transfer.units(), transfer.requestDate(), transfer.status(), transfer.staffId()}).toList(),
                List.of(
                        new ToolbarItem("Request transfer", true, () -> createTransfer()),
                        new ToolbarItem("Approve", false, () -> updateTransfer(TransferStatus.APPROVED)),
                        new ToolbarItem("Dispatch", false, () -> updateTransfer(TransferStatus.IN_TRANSIT)),
                        new ToolbarItem("Receive", false, () -> updateTransfer(TransferStatus.RECEIVED)),
                        new ToolbarItem("Cancel", false, () -> updateTransfer(TransferStatus.CANCELLED))));
    }

    private JPanel reportsPage() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(LIGHT);
        JLabel description = new JLabel("<html>Export current donor, blood-stock, and emergency-request summary metrics.<br>"
                + "CSV files can be opened in spreadsheet software; TXT files are plain text.</html>");
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        description.setForeground(new Color(91, 103, 116));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttons.setOpaque(false);
        JButton csv = primaryButton("Export CSV");
        JButton txt = secondaryButton("Export TXT");
        buttons.add(csv);
        buttons.add(txt);
        csv.addActionListener(event -> exportReport("csv"));
        txt.addActionListener(event -> exportReport("txt"));
        panel.add(description, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return panel;
    }

    private JPanel accountsPage() {
        return tablePage(new String[]{"ID", "Username", "Role", "Active", "Created"},
                () -> services.userAccounts.findAll().stream().map(account -> new Object[]{
                    account.id(), account.username(), account.role(), account.active() ? "Yes" : "No",
                    account.createdAt()}).toList(),
                List.of(
                        new ToolbarItem("Create staff account", true, () -> createStaffAccount()),
                        new ToolbarItem("Activate / deactivate", false, () -> toggleAccount())));
    }

    private JPanel auditPage() {
        return tablePage(new String[]{"ID", "User ID", "Action", "Timestamp", "Description"},
                () -> services.auditLogs.findRecent().stream().map(log -> new Object[]{
                    log.id(), log.userId() == null ? "System" : log.userId(), log.action(), log.timestamp(), log.description()}).toList(),
                List.of(new ToolbarItem("Refresh", false, () -> refreshCurrent(true))));
    }

    private JPanel donorProfilePage() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(LIGHT);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 231, 236)),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)));
        JLabel info = new JLabel("Loading donor profile…");
        JTextField history = new JTextField();
        history.setEditable(false);
        history.setBackground(Color.WHITE);
        history.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        history.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        info.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 8, 16, 8);
        card.add(info, c);
        JButton availability = secondaryButton("Toggle availability");
        c.gridy = 1;
        c.gridwidth = 1;
        card.add(availability, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        card.add(new JLabel("Recent donation history"), c);
        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        card.add(history, c);
        availability.addActionListener(event -> safely(() -> {
            Donor donor = services.donorService.findForUser(user.id());
            services.donorService.setAvailability(donor.id(), !donor.available(), user.id());
            refreshCurrent(true);
        }));
        panel.add(card, BorderLayout.NORTH);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        refreshActions.put(panel, () -> {
            Donor donor = services.donorService.findForUser(user.id());
            info.setText("<html><div style='line-height:1.7'><b>" + escape(donor.fullName()) + "</b><br>"
                    + "Donor ID: " + donor.id() + "<br>Blood group: " + donor.bloodGroup() + "<br>"
                    + "City: " + escape(donor.city()) + "<br>Availability: "
                    + (donor.available() ? "Available" : "Not available") + "<br>Eligibility: "
                    + (services.donorEligibility.checkEligibility(donor, LocalDate.now()) ? "Eligible" : "Not eligible")
                    + "<br>Next eligible date: " + services.donorEligibility.calculateNextEligibleDate(donor)
                    + "<br>Donation count: " + donor.totalDonations() + "</div></html>");
            String recent = services.donations.historyForDonor(donor.id()).stream().limit(5)
                    .map(donation -> donation.donationDate() + " · " + donation.bloodGroup()
                            + " · " + donation.units() + " unit(s)")
                    .collect(java.util.stream.Collectors.joining("     |     "));
            history.setText(recent.isEmpty() ? "No donations recorded yet." : recent);
        });
        return panel;
    }

    private JPanel donorOffersPage() {
        return tablePage(new String[]{"Offer ID", "Request", "Priority", "Blood group", "Units",
                    "Required by", "Location", "Score", "Response"},
                () -> services.donorMatches.findOffers(user.id()).stream().map(offer -> new Object[]{
                    offer.match().id(), offer.match().emergencyRequestId(), offer.emergencyLevel(),
                    offer.bloodGroup(), offer.units(), offer.requiredDate(), offer.location(),
                    offer.match().matchingScore(), offer.response() == null ? "Waiting"
                            : offer.response() ? "Accepted" : "Declined"}).toList(),
                List.of(
                        new ToolbarItem("Accept selected offer", true, () -> respondToOffer(true)),
                        new ToolbarItem("Decline selected offer", false, () -> respondToOffer(false))));
    }

    private JPanel notificationsPage() {
        return tablePage(new String[]{"ID", "Message", "Received", "Read"},
                () -> services.notifications.findForUser(user.id()).stream().map(notification -> new Object[]{
                    notification.id(), notification.message(), notification.createdAt(),
                    notification.read() ? "Yes" : "No"}).toList(),
                List.of(new ToolbarItem("Mark as read", false, () -> markNotificationRead())));
    }

    private JPanel tablePage(String[] headers, java.util.function.Supplier<List<Object[]>> data,
            List<ToolbarItem> toolbarItems) {
        return tablePage(headers, data, toolbarItems, null);
    }

    private JPanel tablePage(String[] headers, java.util.function.Supplier<List<Object[]>> data,
            List<ToolbarItem> toolbarItems, JTextField search) {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(LIGHT);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setOpaque(false);
        if (search != null) {
            search.setPreferredSize(new Dimension(220, 34));
            search.setToolTipText("Search name, email, phone, city, or blood group");
            toolbar.add(search);
        }
        for (ToolbarItem item : toolbarItems) {
            JButton button = item.primary() ? primaryButton(item.title()) : secondaryButton(item.title());
            button.addActionListener(event -> safely(item.action()));
            toolbar.add(button);
        }
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(31);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(238, 242, 246));
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 231, 236)));
        page.add(toolbar, BorderLayout.NORTH);
        page.add(scroll, BorderLayout.CENTER);
        page.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        refreshActions.put(page, () -> {
            model.setRowCount(0);
            for (Object[] row : data.get()) {
                model.addRow(row);
            }
        });
        page.putClientProperty("table", table);
        return page;
    }

    private void createDonor() {
        JTextField name = new JTextField(20);
        JTextField age = new JTextField(20);
        JTextField gender = new JTextField(20);
        JComboBox<BloodGroup> group = new JComboBox<>(BloodGroup.values());
        JTextField phone = new JTextField(20);
        JTextField email = new JTextField(20);
        JTextField address = new JTextField(20);
        JTextField city = new JTextField(20);
        JTextField dob = new JTextField(LocalDate.now().minusYears(25).toString(), 20);
        JPanel panel = fields("Full name", name, "Age", age, "Gender", gender, "Blood group", group,
                "Phone", phone, "Email", email, "Address", address, "City", city, "Date of birth", dob);
        if (JOptionPane.showConfirmDialog(this, panel, "Register donor", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        Donor draft = DonorService.newDonor(name.getText(), Integer.parseInt(age.getText().trim()), gender.getText(),
                (BloodGroup) group.getSelectedItem(), phone.getText(), email.getText(), address.getText(),
                city.getText(), LocalDate.parse(dob.getText().trim()));
        long id = services.donorService.registerByStaff(draft, user.id());
        info("Donor registered. ID: " + id);
        refreshCurrent(true);
    }

    private void toggleDonorAvailability() {
        long id = selectedId("Donors");
        Donor donor = services.donorService.findAll().stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select a donor first."));
        services.donorService.setAvailability(id, !donor.available(), user.id());
        refreshCurrent(true);
    }

    private void toggleDonorStatus() {
        long id = selectedId("Donors");
        Donor donor = services.donorService.findAll().stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select a donor first."));
        DonorStatus next = donor.status() == DonorStatus.ACTIVE ? DonorStatus.INACTIVE : DonorStatus.ACTIVE;
        services.donorService.setStatus(id, next, user.id());
        refreshCurrent(true);
    }

    private void createRequest() {
        List<Hospital> hospitals = services.hospitals.findAll().stream().filter(Hospital::active).toList();
        if (hospitals.isEmpty()) {
            throw new IllegalArgumentException("No active hospitals are registered. Ask an administrator to add one.");
        }
        JComboBox<Hospital> hospital = new JComboBox<>(hospitals.toArray(Hospital[]::new));
        JTextField patient = new JTextField(20);
        JTextField patientId = new JTextField(20);
        JComboBox<BloodGroup> group = new JComboBox<>(BloodGroup.values());
        JTextField units = new JTextField("1", 20);
        JComboBox<EmergencyLevel> level = new JComboBox<>(EmergencyLevel.values());
        JTextField date = new JTextField(LocalDate.now().toString(), 20);
        JTextField time = new JTextField("12:00", 20);
        JTextField location = new JTextField(20);
        JTextField phone = new JTextField(20);
        JPanel panel = fields("Hospital", hospital, "Patient name", patient, "Patient ID", patientId,
                "Required blood group", group, "Units", units, "Priority", level, "Required date", date,
                "Required time (HH:MM)", time, "Hospital location", location, "Contact number", phone);
        if (JOptionPane.showConfirmDialog(this, panel, "Create emergency request",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        Hospital selectedHospital = (Hospital) hospital.getSelectedItem();
        EmergencyRequest request = new EmergencyRequest(0, selectedHospital.id(), patient.getText(), patientId.getText(),
                (BloodGroup) group.getSelectedItem(), Integer.parseInt(units.getText().trim()),
                (EmergencyLevel) level.getSelectedItem(), LocalDate.now(), LocalDate.parse(date.getText().trim()),
                LocalTime.parse(time.getText().trim()), location.getText(), phone.getText(),
                RequestStatus.CREATED, null, user.id());
        long id = services.emergencyRequests.create(request, user.id());
        info("Emergency request created. ID: " + id);
        refreshCurrent(true);
    }

    private void showMatches() {
        long id = selectedId("Emergency requests");
        EmergencyRequest request = request(id);
        info("Searching eligible donors and checking stock. You can continue using the workspace.");
        services.emergencyProcessing.process(request).whenComplete((result, failure) ->
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (failure != null) {
                        safely(() -> {
                            throw new IllegalStateException("Emergency search failed: "
                                    + (failure.getCause() == null ? failure.getMessage() : failure.getCause().getMessage()));
                        });
                        return;
                    }
                    safely(() -> {
                        services.donorMatches.save(id, result.matches(), user.id());
                        services.emergencyRequests.updateStatus(id, RequestStatus.MATCHING, user.id());
                        if (result.matches().isEmpty()) {
                            info("No eligible, available compatible donors were found. "
                                    + "Available exact-group stock: " + result.availableUnits() + " units.");
                            refreshCurrent(true);
                            return;
                        }
                        String rows = result.matches().stream().limit(20)
                                .map(match -> "#" + match.donor().id() + "  " + match.donor().fullName() + "  ·  "
                                        + match.donor().bloodGroup() + "  ·  " + match.donor().city() + "  ·  "
                                        + match.score() + "/100")
                                .collect(java.util.stream.Collectors.joining("\n"));
                        JOptionPane.showMessageDialog(this, rows + "\n\nStock available for exact group: "
                                + result.availableUnits() + " units. Compatibility is a screening aid only. "
                                + "Qualified blood-bank staff must verify component and transfusion compatibility.",
                                "Best donor matches", JOptionPane.INFORMATION_MESSAGE);
                        refreshCurrent(true);
                    });
                }));
    }

    private void changeRequestStatus() {
        long id = selectedId("Emergency requests");
        RequestStatus[] options = {RequestStatus.UNDER_REVIEW, RequestStatus.MATCHING, RequestStatus.CANCELLED,
            RequestStatus.EXPIRED};
        RequestStatus chosen = (RequestStatus) JOptionPane.showInputDialog(this, "Select the request status:",
                "Update request", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (chosen != null) {
            EmergencyRequest current = request(id);
            if (chosen == RequestStatus.CANCELLED && current.status() == RequestStatus.BLOOD_RESERVED) {
                services.inventory.cancelReservation(id, user.id());
                refreshCurrent(true);
                return;
            }
            services.emergencyRequests.updateStatus(id, chosen, user.id());
            refreshCurrent(true);
        }
    }

    private void reserveStock() {
        long requestId = selectedId("Emergency requests");
        EmergencyRequest request = request(requestId);
        List<BloodUnit> units = services.inventory.findAll().stream()
                .filter(unit -> unit.status() == BloodUnitStatus.AVAILABLE).toList();
        if (units.isEmpty()) {
            throw new IllegalArgumentException("No available blood stock is registered.");
        }
        JComboBox<BloodUnit> groupChoice = new JComboBox<>(units.toArray(BloodUnit[]::new));
        groupChoice.setRenderer((list, value, index, selected, focus) -> new JLabel(value == null ? ""
                : value.bloodGroup() + " · " + value.quantity() + " units · Bank " + value.bloodBankId()));
        JTextField count = new JTextField(Integer.toString(request.unitsRequired()), 8);
        JPanel panel = fields("Available stock lot", groupChoice, "Units to reserve", count);
        if (JOptionPane.showConfirmDialog(this, panel, "Reserve stock for request #" + requestId,
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        BloodUnit selected = (BloodUnit) groupChoice.getSelectedItem();
        services.inventory.reserve(requestId, selected.bloodGroup(), request.bloodGroupRequired(),
                Integer.parseInt(count.getText().trim()), user.id());
        info("Compatible stock reserved. The request status is now BLOOD_RESERVED.");
        refreshCurrent(true);
    }

    private void issueStock() {
        List<EmergencyRequest> reserved = services.emergencyRequests.findAll().stream()
                .filter(request -> request.status() == RequestStatus.BLOOD_RESERVED).toList();
        if (reserved.isEmpty()) {
            throw new IllegalArgumentException("There are no reserved requests to issue.");
        }
        JComboBox<EmergencyRequest> choice = new JComboBox<>(reserved.toArray(EmergencyRequest[]::new));
        choice.setRenderer((list, value, index, selected, focus) -> new JLabel(value == null ? ""
                : "#" + value.id() + " · " + value.patientName() + " · " + value.bloodGroupRequired()));
        if (JOptionPane.showConfirmDialog(this, choice, "Select reservation to issue",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            services.inventory.issue(((EmergencyRequest) choice.getSelectedItem()).id(), user.id());
            info("Reserved blood issued and request marked fulfilled.");
            refreshCurrent(true);
        }
    }

    private void addStock() {
        List<BloodBank> banks = services.bloodBanks.findAll().stream().filter(BloodBank::active).toList();
        if (banks.isEmpty()) {
            throw new IllegalArgumentException("Register a blood bank before adding stock.");
        }
        JComboBox<BloodBank> bank = new JComboBox<>(banks.toArray(BloodBank[]::new));
        JComboBox<BloodGroup> group = new JComboBox<>(BloodGroup.values());
        JTextField quantity = new JTextField("1", 20);
        JTextField collected = new JTextField(LocalDate.now().toString(), 20);
        JTextField expires = new JTextField(LocalDate.now().plusDays(35).toString(), 20);
        JTextField location = new JTextField("Storage", 20);
        JPanel panel = fields("Blood bank", bank, "Blood group", group, "Quantity", quantity,
                "Collection date", collected, "Expiry date", expires, "Storage location", location);
        if (JOptionPane.showConfirmDialog(this, panel, "Add blood stock",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        BloodBank selectedBank = (BloodBank) bank.getSelectedItem();
        BloodUnit unit = new BloodUnit(0, (BloodGroup) group.getSelectedItem(),
                LocalDate.parse(collected.getText().trim()), LocalDate.parse(expires.getText().trim()),
                Integer.parseInt(quantity.getText().trim()), location.getText().trim(), selectedBank.id(),
                BloodUnitStatus.AVAILABLE);
        services.inventory.add(unit, user.id());
        info("Blood stock added.");
        refreshCurrent(true);
    }

    private void addHospital() {
        JTextField name = new JTextField(20);
        JTextField address = new JTextField(20);
        JTextField city = new JTextField(20);
        JTextField phone = new JTextField(20);
        JTextField email = new JTextField(20);
        JTextField emergency = new JTextField(20);
        JPanel panel = fields("Hospital name", name, "Address", address, "City", city, "Phone", phone,
                "Email", email, "Emergency contact", emergency);
        if (JOptionPane.showConfirmDialog(this, panel, "Register hospital",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            services.hospitals.create(new Hospital(0, name.getText(), address.getText(), city.getText(),
                    phone.getText(), email.getText(), emergency.getText(), true));
            info("Hospital registered.");
            refreshCurrent(true);
        }
    }

    private void addBank() {
        JTextField name = new JTextField(20);
        JTextField address = new JTextField(20);
        JTextField city = new JTextField(20);
        JTextField phone = new JTextField(20);
        JTextField email = new JTextField(20);
        JTextField capacity = new JTextField("100", 20);
        JPanel panel = fields("Blood bank name", name, "Address", address, "City", city,
                "Phone", phone, "Email", email, "Storage capacity", capacity);
        if (JOptionPane.showConfirmDialog(this, panel, "Register blood bank",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            services.bloodBanks.create(new BloodBank(0, name.getText(), address.getText(), city.getText(),
                    phone.getText(), email.getText(), Integer.parseInt(capacity.getText().trim()), 0, true));
            info("Blood bank registered.");
            refreshCurrent(true);
        }
    }

    private void createTransfer() {
        List<BloodBank> banks = services.bloodBanks.findAll().stream().filter(BloodBank::active).toList();
        if (banks.size() < 2) {
            throw new IllegalArgumentException("At least two active blood banks are needed for a transfer.");
        }
        JComboBox<BloodBank> source = new JComboBox<>(banks.toArray(BloodBank[]::new));
        JComboBox<BloodBank> destination = new JComboBox<>(banks.toArray(BloodBank[]::new));
        JComboBox<BloodGroup> group = new JComboBox<>(BloodGroup.values());
        JTextField units = new JTextField("1", 20);
        JPanel panel = fields("Source", source, "Destination", destination, "Blood group", group, "Units", units);
        if (JOptionPane.showConfirmDialog(this, panel, "Request blood transfer",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        BloodBank from = (BloodBank) source.getSelectedItem();
        BloodBank to = (BloodBank) destination.getSelectedItem();
        services.transfers.create(new BloodTransfer(0, from.id(), to.id(), (BloodGroup) group.getSelectedItem(),
                Integer.parseInt(units.getText().trim()), LocalDate.now(), TransferStatus.REQUESTED, null, user.id()));
        info("Transfer request submitted for approval.");
        refreshCurrent(true);
    }

    private void respondToOffer(boolean accepted) {
        long matchId = selectedId("Donation offers");
        services.donorMatches.respond(matchId, user.id(), accepted);
        info(accepted ? "Offer accepted. The blood bank will coordinate next steps."
                : "Offer declined.");
        refreshCurrent(true);
    }

    private void markNotificationRead() {
        long id = selectedId("Notifications");
        services.notifications.markRead(id, user.id());
        refreshCurrent(true);
    }

    private void updateTransfer(TransferStatus status) {
        long id = selectedId("Blood transfers");
        services.transfers.updateStatus(id, status, user.id());
        info("Transfer #" + id + " updated to " + status + ".");
        refreshCurrent(true);
    }

    private void toggleAccount() {
        long id = selectedId("Accounts");
        var account = services.userAccounts.findAll().stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select an account first."));
        if (account.id() == user.id()) {
            throw new IllegalArgumentException("You cannot deactivate the account you are currently using.");
        }
        services.accounts.setActive(id, !account.active(), user.id());
        refreshCurrent(true);
    }

    private void createStaffAccount() {
        JTextField username = new JTextField(20);
        javax.swing.JPasswordField password = new javax.swing.JPasswordField(20);
        JPanel panel = fields("Username", username, "Password (12+ characters)", password);
        if (JOptionPane.showConfirmDialog(this, panel, "Create blood-bank staff account",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        char[] secret = password.getPassword();
        try {
            long id = services.accounts.createStaff(username.getText(), secret, user.id());
            info("Staff account created. ID: " + id);
            refreshCurrent(true);
        } finally {
            java.util.Arrays.fill(secret, '\0');
            password.setText("");
        }
    }

    private void exportReport(String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("blood-management-report." + extension));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path target = chooser.getSelectedFile().toPath();
            if (!target.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith("." + extension)) {
                target = target.resolveSibling(target.getFileName() + "." + extension);
            }
            services.reports.exportSummary(target);
            info("Report exported to " + target.toAbsolutePath());
        }
    }

    private JPanel fields(Object... fields) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(4, 7, 4, 7);
        c.anchor = GridBagConstraints.WEST;
        for (int index = 0; index < fields.length; index += 2) {
            c.gridx = 0;
            panel.add(new JLabel(fields[index].toString()), c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add((Component) fields[index + 1], c);
            c.gridy++;
        }
        return panel;
    }

    private void addFormRow(JPanel panel, int row, String label, Component component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(9, 8, 9, 8);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, c);
    }

    private long selectedId(String tabName) {
        Component selected = tabs.getSelectedComponent();
        if (selected == null || !tabName.equals(tabs.getTitleAt(tabs.getSelectedIndex()))) {
            throw new IllegalArgumentException("Open the " + tabName + " tab and select a row first.");
        }
        Object tableObject = ((JPanel) selected).getClientProperty("table");
        if (!(tableObject instanceof JTable table) || table.getSelectedRow() < 0) {
            throw new IllegalArgumentException("Select a row first.");
        }
        return Long.parseLong(table.getValueAt(table.getSelectedRow(), 0).toString());
    }

    private EmergencyRequest request(long id) {
        return services.emergencyRequests.findAll().stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Emergency request not found."));
    }

    private void refreshCurrent() {
        refreshCurrent(false);
    }

    private void refreshCurrent(boolean force) {
        Component current = tabs.getSelectedComponent();
        if (current == null) {
            return;
        }
        Runnable refresh = refreshActions.get(current);
        if (refresh == null) {
            return;
        }
        if (!force && Boolean.TRUE.equals(((JPanel) current).getClientProperty("loaded"))) {
            return;
        }
        safely(() -> {
            refresh.run();
            ((JPanel) current).putClientProperty("loaded", Boolean.TRUE);
        });
    }

    private JPanel statCard(String label, String value) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 231, 236)),
                BorderFactory.createEmptyBorder(18, 19, 18, 19)));
        JLabel number = new JLabel(value);
        number.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        number.setForeground(RED);
        JLabel caption = new JLabel(label);
        caption.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        caption.setForeground(new Color(101, 113, 124));
        card.add(number, BorderLayout.CENTER);
        card.add(caption, BorderLayout.SOUTH);
        return card;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(RED);
        button.setForeground(Color.WHITE);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setFocusPainted(false);
        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(NAVY);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setFocusPainted(false);
        return button;
    }

    private String roleLabel(UserRole role) {
        return switch (role) {
            case ADMIN -> "ADMINISTRATOR";
            case BLOOD_BANK_STAFF -> "BLOOD BANK STAFF";
            case DONOR -> "DONOR";
        };
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Emergency Blood Management", JOptionPane.INFORMATION_MESSAGE);
    }

    private void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null ? "An unexpected error occurred." : exception.getMessage();
            JOptionPane.showMessageDialog(this, message, "Operation could not be completed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private record ToolbarItem(String title, boolean primary, Runnable action) {
    }
}
