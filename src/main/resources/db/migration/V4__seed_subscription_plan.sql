-- Planes iniciales. Los precios son un punto de partida en COP: ajustar
-- antes de salir a produccion (basta un UPDATE en una migracion nueva).
INSERT INTO subscription_plan
    (code, name, description, monthly_price, trial_days, max_members,
     max_active_cases, marketplace_enabled, white_label_enabled, sort_order)
VALUES
    ('FREEMIUM',
     'Freemium',
     'Para el abogado que trabaja solo. Gestion de casos y clientes, sin directorio publico.',
     0, 0, 1, 10, FALSE, FALSE, 1),

    ('PROFESIONAL',
     'Profesional',
     'Para firmas pequenas. Hasta 5 miembros, casos ilimitados y ficha en el directorio publico.',
     89000, 14, 5, NULL, TRUE, FALSE, 2),

    ('FIRMA',
     'Firma',
     'Miembros ilimitados, directorio publico y portal del cliente con la marca de la firma.',
     249000, 14, NULL, NULL, TRUE, TRUE, 3);
