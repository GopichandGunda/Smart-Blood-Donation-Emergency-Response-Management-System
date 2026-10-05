USE emergency_blood_management;

INSERT INTO hospitals (name, address, city, phone, email, emergency_contact)
SELECT 'Central City General Hospital', '100 Health Avenue', 'Hyderabad', '+91 40 4000 1000',
       'blooddesk@example.org', '+91 40 4000 1001'
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE email = 'blooddesk@example.org');

INSERT INTO blood_banks (name, address, city, phone, email, storage_capacity)
SELECT 'Central City Blood Bank', '12 Wellness Road', 'Hyderabad', '+91 40 4000 2000',
       'bloodbank@example.org', 500
WHERE NOT EXISTS (SELECT 1 FROM blood_banks WHERE email = 'bloodbank@example.org');

INSERT INTO blood_units (blood_group, collection_date, expiry_date, quantity, storage_location, blood_bank_id, status)
SELECT 'O+', CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 35 DAY), 6, 'Refrigerator A / Shelf 1', b.id, 'AVAILABLE'
FROM blood_banks b
WHERE b.email = 'bloodbank@example.org'
  AND NOT EXISTS (
      SELECT 1 FROM blood_units u
      WHERE u.blood_bank_id = b.id AND u.storage_location = 'Refrigerator A / Shelf 1'
  );
