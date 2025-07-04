const express = require('express');
const SibApiV3Sdk = require('sib-api-v3-sdk');
const https = require('https');
require('dotenv').config();

const app = express();
app.use(express.json());

// Configure Brevo
const defaultClient = SibApiV3Sdk.ApiClient.instance;
const apiKey = defaultClient.authentications['api-key'];
apiKey.apiKey = process.env.BREVO_API_KEY;
const senderEmail = process.env.SENDER_EMAIL;

// Add startup validation and logging
const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
    console.log(`[SERVER] Running on port ${PORT}`);
    console.log(`[SERVER] Brevo API Key: ${process.env.BREVO_API_KEY ? 'Exists' : 'MISSING!'}`);
    console.log(`[SERVER] Sender Email: ${process.env.SENDER_EMAIL || 'NOT SET!'}`);
});

//initial stage email notification to FH and requester
app.post('/send-ride-email', async (req, res) => {
    console.log(`[EMAIL NOTIFY] Endpoint hit at ${new Date().toISOString()}`);
    
    const { 
        requesterEmail,
        requesterName,
        approverEmail,
        approverName,
        approverRole,
        requestId 
    } = req.body;

    // Validate required fields
    if (!requesterEmail || !approverEmail || !requestId) {
        console.log(`[EMAIL NOTIFY] Missing required fields`);
        return res.status(400).json({ error: 'Missing required fields' });
    }

    // Log incoming request data
    console.log(`[EMAIL NOTIFY] Request ID: ${requestId}`);
    console.log(`[EMAIL NOTIFY] Requester: ${requesterEmail} (${requesterName})`);
    console.log(`[EMAIL NOTIFY] Approver: ${approverEmail} (${approverName}) - ${approverRole}`);

    try {
        // Validate environment variables
        if (!process.env.BREVO_API_KEY || !senderEmail) {
            throw new Error('Missing Brevo configuration');
        }

        // Email to Requester
        const requesterEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        requesterEmailObj.sender = { 
            name: "Cab Approval System", 
            email: senderEmail 
        };
        requesterEmailObj.to = [{ email: requesterEmail }];
        requesterEmailObj.subject = `Ride Request #${requestId} Submitted`;
        requesterEmailObj.htmlContent = `
           <h3>Your Ride Request Has Been Submitted</h3>
            <p>Hello ${requesterName || 'User'},</p>
            <p>Request ID: ${requestId}</p>
            <p>We've notified ${approverName || 'your approver'} (${approverRole || 'Approver'}) and will update you once it's reviewed.</p>
        `;

        // Email to Approver
        const approverEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        approverEmailObj.sender = { 
            name: "Cab Approval System", 
            email: senderEmail 
        };
        approverEmailObj.to = [{ email: approverEmail }];
        approverEmailObj.subject = `Approval Needed for Ride Request #${requestId}`;
        approverEmailObj.htmlContent = `
           <h3>New Ride Request Requires Approval</h3>
            <p>Hello ${approverName || 'Approver'},</p>
            <p>Request ID: ${requestId} from ${requesterName || 'User'} needs your approval as ${approverRole || 'Approver'}.</p>
            <p>Please review this request at your earliest convenience.</p>
        `;

        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();

        console.log(`[EMAIL NOTIFY] Sending emails...`);

        // Send to requester
        await apiInstance.sendTransacEmail(requesterEmailObj);
        console.log(`[EMAIL NOTIFY] ✅ Requester email sent to: ${requesterEmail}`);

        // Send to approver
        await apiInstance.sendTransacEmail(approverEmailObj);
        console.log(`[EMAIL NOTIFY] ✅ Approver email sent to: ${approverEmail}`);

        res.status(200).json({ 
            success: true, 
            message: 'Emails sent successfully',
            requestId: requestId
        });

    } catch (error) {
        console.error('[EMAIL NOTIFY] ❌ Error:', error.message);
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send emails',
            details: error.message 
        });
    }
});

//Email notification to HR and requester on FH approval 
app.post('/send-fh-approval-email', async (req, res) => {
    console.log(`[FH APPROVAL] Endpoint hit at ${new Date().toISOString()}`);
    console.log(`Sending mail to HR and Requester on approval by FH`);
    
    const { 
        requesterEmail,
        requesterName,
        approverEmail,
        approverName,
        requestId,
        empId,
        hrEmail,
        hrName
    } = req.body;

    console.log(`[EMAIL NOTIFY] ✅ Requester email sent to: ${requesterEmail}`);
    console.log(`[EMAIL NOTIFY] ✅ HR email sent to: ${hrEmail}`);

     if (!hrEmail) {
        return res.status(400).json({ error: 'HR email not found for this FH' });
    }

    if (!requesterEmail || !approverEmail || !requestId || !hrEmail) {
        return res.status(400).json({ error: 'Missing required fields' });
    }

    try {
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();

        // Email to Requester
        const requesterEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        requesterEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        requesterEmailObj.to = [{ email: requesterEmail }];
        requesterEmailObj.subject = `✅ Ride Request #${requestId} Approved by FH`;
        requesterEmailObj.htmlContent = `
            <h3>Your Ride Request Has Been Approved by Functional Head</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab request (ID: <strong>${requestId}</strong>) has been approved by ${approverName} (FH).</p>
            <p>It will now be processed by HR for final approval.</p>
        `;

       const hrEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        hrEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        hrEmailObj.to = [{ email: hrEmail }];
        hrEmailObj.subject = `HR Approval Needed for Ride Request #${requestId}`;
        hrEmailObj.htmlContent = `
            <h3>Approval Required from ${hrName} HR Head</h3>
            <p>Employee: ${requesterName} (${empId})</p>
            <p>Request ID: <strong>${requestId}</strong></p>
            <p>Approved by FH: ${approverName}</p>
            <p>Please review and approve this request.</p>
        `;

        await apiInstance.sendTransacEmail(requesterEmailObj);
        await apiInstance.sendTransacEmail(hrEmailObj);

        res.status(200).json({ 
            success: true, 
            message: 'FH approval emails sent successfully',
            requestId: requestId
        });

    } catch (error) {
        console.error('[FH APPROVAL] ❌ Error:', error.message);
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send FH approval emails',
            details: error.message 
        });
    }
});

//email notication of Rejection of approval by FH to requester
app.post('/send-fh-rejection-email', async (req, res) => {
    console.log(`[FH Rejection] Endpoint hit at ${new Date().toISOString()}`);
    console.log(`Sending mail to Requester on rejection by FH`);
    
    const { 
        requesterEmail,
        requesterName,
        approverEmail,
        approverName,
        requestId,
        empId,
        rejectionReason
    } = req.body;

    console.log(`[EMAIL NOTIFY] ✅ Requester email sent to: ${requesterEmail}`);
    
    try {
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();
        
        const emailObj = new SibApiV3Sdk.SendSmtpEmail();
        emailObj.sender = { name: "Cab Approval System", email: senderEmail };
        emailObj.to = [{ email: requesterEmail }];
        emailObj.subject = `Ride Request #${requestId} Rejected`;
        emailObj.htmlContent = `
            <h3>Request Rejected by Functional Head</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab request (ID: ${requestId}) has been rejected by ${approverName}.</p>
            <p><strong>Reason:</strong> ${rejectionReason}</p>
        `;

        await apiInstance.sendTransacEmail(emailObj);
        res.status(200).json({ success: true });
    } catch (error) {
        console.error('Rejection email error:', error);
        res.status(500).json({ error: 'Email sending failed' });
    }
});

// HR Approval for regular employee Endpoint (uses vendorEmail and vendorName from request body)
app.post('/send-hr-approval-email-regular', async (req, res) => {
    console.log(`[HR APPROVAL] Endpoint hit at ${new Date().toISOString()}`);
    console.log(`Sending mail to Requester , FH and Vendor on approval by HR`);
    
    const { 
        requesterEmail,
        requesterName,
        approverEmail,
        approverName,
        requestId,
        empId,
        fhApproverName,
        fhApproverEmail,
        vendorEmail,
        vendorName
    } = req.body;

    console.log(`[EMAIL NOTIFY] ✅ Requester email sent to: ${requesterEmail}`);
    console.log(`[EMAIL NOTIFY] ✅ FH email sent to: ${fhApproverEmail}`);
    console.log(`[EMAIL NOTIFY] ✅ vendor email sent to: ${vendorEmail}`);

   if (!requesterEmail || !requestId || !vendorEmail || !vendorName ||
        !approverEmail || !approverName || !fhApproverName || !fhApproverEmail) {
        return res.status(400).json({ error: 'Missing required fields for regular HR approval' });
    }
    
    try {
        console.log(`[HR APPROVAL] Sending email to ${requesterEmail}`);
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();
        const emailsToSend = [];
        // Email to Requester
        // 1. Requester Email (always sent)
        const requesterEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        requesterEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        requesterEmailObj.to = [{ email: requesterEmail }];
        requesterEmailObj.subject = `🎉 Ride Request #${requestId} - APPROVED!`;
        requesterEmailObj.htmlContent = `
            <h3>Final Approval Received!</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab request (ID: ${requestId}) has been fully approved.</p>
            <p>The vendor (${vendorName}) will contact you soon.</p>
        `;
        emailsToSend.push(apiInstance.sendTransacEmail(requesterEmailObj));

        // 2. Vendor Email (always sent)
        const vendorEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        vendorEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        vendorEmailObj.to = [{ email: vendorEmail }];
        vendorEmailObj.subject = `🚕 New Cab Assignment - Request #${requestId}`;
        vendorEmailObj.htmlContent = `
            <h3>Cab Assignment Required</h3>
            <p><strong>Request ID:</strong> ${requestId}</p>
            <p><strong>Employee:</strong> ${requesterName} (${empId})</p>
            <p>Approved by HR: ${approverName}</p>
        `;
        emailsToSend.push(apiInstance.sendTransacEmail(vendorEmailObj));

       // 3. FH Email (conditional - only for regular employees)
        const fhEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        fhEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        fhEmailObj.to = [{ email: fhApproverEmail }];
        fhEmailObj.subject = `Ride Request #${requestId} Approved by HR`;
        fhEmailObj.htmlContent = `
            <h3>HR Approved Your Request</h3>
            <p>Hello ${fhApproverName},</p>
            <p>The request you approved (ID: ${requestId}) has been finalized by HR.</p>
            <p>Vendor: ${vendorName} (${vendorEmail})</p>
        `;
        emailsToSend.push(apiInstance.sendTransacEmail(fhEmailObj));

        // Send all emails concurrently
        await Promise.all(emailsToSend);

        res.status(200).json({ 
            success: true, 
            message: 'HR approval emails sent successfully',
            requestId: requestId,
            emailsSent: emailsToSend.length
        });

    } catch (error) {
        console.error('[HR APPROVAL] ❌ Error:', error.message);
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send HR approval emails',
            details: error.message 
        });
    }
});

// HR Approval Endpoint for FH as an employee (uses vendorEmail and vendorName from request body)
app.post('/send-hr-approval-email-fh-as-employee', async (req, res) => {
    console.log(`[HR APPROVAL - FH AS EMPLOYEE] Endpoint hit at ${new Date().toISOString()}`);

    const { 
        requesterEmail, 
        requesterName, 
        approverEmail, 
        approverName, 
        requestId, 
        empId,
        vendorEmail,
        vendorName
    } = req.body;

    console.log(`[EMAIL NOTIFY] ✅ Requester(FH) email sent to: ${requesterEmail}`);
    console.log(`[EMAIL NOTIFY] ✅ vendor email sent to: ${vendorEmail}`);

    // Validate required fields for FH-as-employee flow
    if (!requesterEmail || !requestId || !vendorEmail || !vendorName ||
        !approverEmail || !approverName) {
        return res.status(400).json({ error: 'Missing required fields for FH-as-employee HR approval' });
    }

    try {
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();
        const emailsToSend = [];

        // Email to Requester (FH as employee)
        const requesterEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        requesterEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        requesterEmailObj.to = [{ email: requesterEmail }];
        requesterEmailObj.subject = `🎉 Ride Request #${requestId} - APPROVED!`;
        requesterEmailObj.htmlContent = `
            <h3>Final Approval Received!</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab request (ID: ${requestId}) has been fully approved by HR.</p>
            <p>The vendor (${vendorName}) will contact you soon.</p>
        `;
        emailsToSend.push(apiInstance.sendTransacEmail(requesterEmailObj));

        // Email to Vendor
        const vendorEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        vendorEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        vendorEmailObj.to = [{ email: vendorEmail }];
        vendorEmailObj.subject = `🚕 New Cab Assignment - Request #${requestId}`;
        vendorEmailObj.htmlContent = `
            <h3>Cab Assignment Required</h3>
            <p><strong>Request ID:</strong> ${requestId}</p>
            <p><strong>Employee:</strong> ${requesterName} (${empId})</p>
            <p>Approved by HR: ${approverName}</p>
        `;
        emailsToSend.push(apiInstance.sendTransacEmail(vendorEmailObj));

        await Promise.all(emailsToSend);

        res.status(200).json({ 
            success: true, 
            message: 'HR approval emails (FH as employee) sent successfully',
            requestId: requestId,
            emailsSent: emailsToSend.length
        });

    } catch (error) {
        console.error('[HR APPROVAL - FH AS EMPLOYEE] ❌ Error:', error.message);
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send HR approval emails (FH as employee)',
            details: error.message 
        });
    }
});

//Request rejection by HR : mail sent to FH and requester 
app.post('/send-hr-rejection-email/regular', async (req, res) => {
    console.log(`[HR REJECTION - REGULAR EMPLOYEE] Endpoint hit at ${new Date().toISOString()}`);
    const { 
        requesterEmail, 
        requesterName, 
        approverEmail, 
        approverName,
        requestId, 
        empId, 
        rejectionReason,
        fhApproverEmail, 
        fhApproverName
    } = req.body;
    
    console.log(`[EMAIL NOTIFY] ✅ Requester(FH) email sent to: ${requesterEmail}`);
    console.log(`[EMAIL NOTIFY] ✅ FH email sent to: ${fhApproverEmail}`);
    
    try {
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();
        const emailsToSend = [];

        // 1. Email to requester
        const requesterEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        requesterEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        requesterEmailObj.to = [{ email: requesterEmail }];
        requesterEmailObj.subject = `❌ Request #${requestId} Rejected by HR`;
        requesterEmailObj.htmlContent = `
            <h3>Request Rejected</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab request (ID: ${requestId}) has been rejected by HR.</p>
            <p><strong>Reason:</strong> ${rejectionReason}</p>
        `;
        emailsToSend.push(apiInstance.sendTransacEmail(requesterEmailObj));

        // 2. Email to FH approver
        const fhEmailObj = new SibApiV3Sdk.SendSmtpEmail();
        fhEmailObj.sender = { name: "Cab Approval System", email: senderEmail };
        fhEmailObj.to = [{ email: fhApproverEmail }];
        fhEmailObj.subject = `Request #${requestId} Rejected by HR`;
        fhEmailObj.htmlContent = `
            <h3>HR Rejected Your Approved Request</h3>
            <p>Hello ${fhApproverName},</p>
            <p>The request you approved (ID: ${requestId}) has been rejected by HR.</p>
            <p><strong>Reason:</strong> ${rejectionReason}</p>
        `;
        emailsToSend.push(apiInstance.sendTransacEmail(fhEmailObj));

        await Promise.all(emailsToSend);
        res.status(200).json({ success: true });
    } catch (error) {
        console.error("[HR REJECTION - REGULAR] Error:", error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// FH-as-employee HR rejection
app.post('/send-hr-rejection-email/fh-as-employee', async (req, res) => {
    console.log(`[HR REJECTION - FH AS EMPLOYEE] Endpoint hit at ${new Date().toISOString()}`);
    const { 
        requesterEmail, 
        requesterName, 
        approverEmail, 
        approverName,
        requestId, 
        empId, 
        rejectionReason
    } = req.body;

    console.log(`[EMAIL NOTIFY] ✅ Requester(FH) email sent to: ${requesterEmail}`);
    console.log(`[EMAIL NOTIFY] ✅ FH email sent to: ${approverEmail}`);
    try {
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();
        
        // Only send to requester (who is FH)
        const emailObj = new SibApiV3Sdk.SendSmtpEmail();
        emailObj.sender = { name: "Cab Approval System", email: senderEmail };
        emailObj.to = [{ email: requesterEmail }];
        emailObj.subject = `❌ Request #${requestId} Rejected by HR`;
        emailObj.htmlContent = `
            <h3>Request Rejected</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab request (ID: ${requestId}) has been rejected by HR.</p>
            <p><strong>Reason:</strong> ${rejectionReason}</p>
        `;
        
        await apiInstance.sendTransacEmail(emailObj);
        res.status(200).json({ success: true });
    } catch (error) {
        console.error("[HR REJECTION - FH] Error:", error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// For HR/FH notification (cab details sent to requester)
app.post('/send-assignment-notification', async (req, res) => {
    console.log(`[ASSIGNMENT NOTIFY] Endpoint hit at ${new Date().toISOString()}`);
    
    const { 
        toEmail,
        requesterName,
        requestId
    } = req.body;

    // Validate required fields
    if (!toEmail || !requestId) {
        return res.status(400).json({ error: 'Missing required fields' });
    }

    try {
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();
        
        const emailObj = new SibApiV3Sdk.SendSmtpEmail();
        emailObj.sender = { name: "Cab Approval System", email: senderEmail };
        emailObj.to = [{ email: toEmail }];
        emailObj.subject = `Cab Details Sent for Request #${requestId}`;
        emailObj.htmlContent = `
            <h3>Cab Details Delivered</h3>
            <p>Cab details for ${requesterName}'s request (ID: ${requestId}) have been sent to the employee.</p>
            <p>The vendor will coordinate directly with the requester.</p>
        `;

        await apiInstance.sendTransacEmail(emailObj);
        
        res.status(200).json({ 
            success: true,
            message: 'Assignment notification sent',
            recipient: toEmail,
            requestId: requestId
        });
    } catch (error) {
        console.error('[ASSIGNMENT NOTIFY] Error:', error.message);
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send assignment notification',
            details: error.message 
        });
    }
});

// For requester notification (with driver details)
app.post('/send-requester-notification', async (req, res) => {
    console.log(`[REQUESTER NOTIFY] Endpoint hit at ${new Date().toISOString()}`);
    
    const { 
        toEmail,
        requesterName,
        driverName,
        driverMobile,
        cabNumber,
        requestId
    } = req.body;

    // Validate required fields
    const requiredFields = ['toEmail', 'requesterName', 'driverName', 'driverMobile', 'cabNumber', 'requestId'];
    const missingFields = requiredFields.filter(field => !req.body[field]);
    
    if (missingFields.length > 0) {
        return res.status(400).json({ 
            error: `Missing required fields: ${missingFields.join(', ')}` 
        });
    }

    try {
        const apiInstance = new SibApiV3Sdk.TransactionalEmailsApi();
        
        const emailObj = new SibApiV3Sdk.SendSmtpEmail();
        emailObj.sender = { name: "Cab Approval System", email: senderEmail };
        emailObj.to = [{ email: toEmail }];
        emailObj.subject = `🚕 Your Cab Details for Request #${requestId}`;
        emailObj.htmlContent = `
            <h3>Your Cab Has Been Assigned!</h3>
            <p>Hello ${requesterName},</p>
            <p>Your cab for request <strong>#${requestId}</strong> has been assigned:</p>
            <p><strong>Driver:</strong> ${driverName}</p>
            <p><strong>Contact:</strong> ${driverMobile}</p>
            <p><strong>Cab Number:</strong> ${cabNumber}</p>
            <p>The driver will contact you directly for pickup coordination.</p>
        `;

        await apiInstance.sendTransacEmail(emailObj);
        
        res.status(200).json({ 
            success: true,
            message: 'Requester notification sent',
            requestId: requestId
        });
    } catch (error) {
        console.error('[REQUESTER NOTIFY] Error:', error.message);
        res.status(500).json({ 
            success: false, 
            error: 'Failed to send requester notification',
            details: error.message 
        });
    }
});

// Health check endpoint
app.get('/', (req, res) => {
    res.json({ 
        status: 'running',
        timestamp: new Date().toISOString(),
        service: 'Email Notification Service'
    });
});

setInterval(() => {
  https.get("https://cab-approval-system.onrender.com");
}, 300000); 
// Fixed catch-all route with named parameter
app.use('/*all', (req, res) => {
    console.log(`[SERVER] Unhandled request: ${req.method} ${req.originalUrl}`);
    res.status(404).json({ error: 'Endpoint not found' });
});
