-- Veículo de demo para testes rápidos — Ford Ranger Raptor (VIN real de exemplo)
INSERT INTO vehicles (vin, model, year, owner_id, created_at)
VALUES ('1FTFW1ET5EKE36050', 'Ford Ranger Raptor', 2024, 'mechanic-demo', NOW())
ON CONFLICT (vin) DO NOTHING;
