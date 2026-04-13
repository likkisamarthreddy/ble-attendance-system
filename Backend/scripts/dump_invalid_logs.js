const mongoose = require('mongoose');
require('dotenv').config();

const auditSchema = new mongoose.Schema({}, { strict: false });
const AuditLog = mongoose.model('AuditLog', auditSchema, 'audit_logs');

const SLOT_DURATION_MS = 7000;

async function dumpLogs() {
    await mongoose.connect(process.env.DATABASE_URL);
    const currentSlot = Math.floor(Date.now() / SLOT_DURATION_MS);
    console.log("Current Server Slot:", currentSlot);
    console.log("Current Server Time:", new Date().toISOString());

    const logs = await AuditLog.find({ action: 'TOKEN_INVALID' }).sort({ createdAt: -1 }).limit(5);
    console.log(JSON.stringify(logs, null, 2));
    await mongoose.connection.close();
}

dumpLogs();
