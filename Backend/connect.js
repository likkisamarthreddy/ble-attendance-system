const prisma = require("./prisma/client");

async function connectDatabase() {
    try {
        await prisma.$connect();
        console.log("PostgreSQL database connected via Prisma");
    } catch (err) {
        console.error("PostgreSQL Connection Error:", err.message);
        process.exit(1);
    }
}

module.exports = connectDatabase;