require("dotenv").config();
const { Client } = require("pg");

const DB_URL = "postgresql://neondb_owner:npg_eonl5DUd7kVN@ep-ancient-fog-a1s5pere.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";

const client = new Client({
  connectionString: DB_URL,
});

async function main() {
  console.log("Seeding database via direct PostgreSQL client (`pg`)...");
  
  await client.connect();
  console.log("✅ Attached to Database");

  try {
    // Insert Admins
    await client.query(`
      INSERT INTO admins (name, email, uid, college, is_disabled, created_at)
      VALUES 
        ('System Admin', 'admin@gmail.com', 'Yw2nvRa4UnhQkXlw7h4clHvk5Ei1', 'Global Institute', false, NOW()),
        ('Samarth Reddy (Admin)', 'likkisamarth@gmail.com', 'Uwammx38uTVz30gEeUBYzzxT8TR2', 'Global Institute', false, NOW())
      ON CONFLICT (email) DO NOTHING;
    `);
    console.log("✅ Admins created: admin@gmail.com, likkisamarth@gmail.com");

    // Insert Professor
    await client.query(`
      INSERT INTO professors (name, email, uid, is_disabled, created_at)
      VALUES 
        ('Professor Sir', 'sir@gmail.com', 'ke2sz1hcnyLFlnCfLgfSEajwd1D3', false, NOW())
      ON CONFLICT (email) DO NOTHING;
    `);
    console.log("✅ Professor created: sir@gmail.com");

  } catch (error) {
    console.error("❌ Error seeding users:", error.message);
  } finally {
    await client.end();
  }
}

main();
