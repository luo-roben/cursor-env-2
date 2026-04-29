import React, { useState } from 'react';
import { Form, Input, Select, Button, Card, message } from 'antd';
import { SendOutlined } from '@ant-design/icons';
import { submitReview } from '../../services/api';
import { useNavigate } from 'react-router-dom';

const { TextArea } = Input;
const { Option } = Select;

const ReviewSubmit: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [documentType, setDocumentType] = useState<string>('');
  const navigate = useNavigate();

  const handleSubmit = async (values: any) => {
    try {
      setLoading(true);
      const payload = {
        ...values,
        tenantId: 1,
        submittedBy: 1,
      };
      const res = await submitReview(payload);
      message.success('审查提交成功');
      const taskId = res.data?.data?.id || res.data?.id;
      if (taskId) {
        navigate(`/review/${taskId}`);
      } else {
        navigate('/review/list');
      }
    } catch (err) {
      message.error('提交审查失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>提交审查</h2>
      <Card style={{ maxWidth: 800 }}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ documentType: '', contentType: '', channel: '' }}
        >
          <Form.Item
            name="documentType"
            label="文档类型"
            rules={[{ required: true, message: '请选择文档类型' }]}
          >
            <Select
              placeholder="请选择文档类型"
              onChange={(v) => setDocumentType(v)}
              size="large"
            >
              <Option value="marketing">营销材料 (Marketing)</Option>
              <Option value="contract">合同 (Contract)</Option>
              <Option value="prospectus">募集说明书 (Prospectus)</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="contentType"
            label="内容类型"
            rules={[{ required: true, message: '请输入内容类型' }]}
          >
            <Input placeholder="请输入内容类型，如：基金推介、风险揭示等" size="large" />
          </Form.Item>

          <Form.Item name="productType" label="产品类型">
            <Input placeholder="请输入产品类型，如：公募基金、私募基金等" size="large" />
          </Form.Item>

          {documentType === 'contract' && (
            <Form.Item
              name="contractType"
              label="合同类型"
              rules={[{ required: true, message: '请选择合同类型' }]}
            >
              <Select placeholder="请选择合同类型" size="large">
                <Option value="fund_contract">基金合同</Option>
                <Option value="custody_agreement">托管协议</Option>
                <Option value="sales_agreement">销售协议</Option>
                <Option value="advisory_agreement">顾问协议</Option>
              </Select>
            </Form.Item>
          )}

          <Form.Item name="channel" label="渠道">
            <Input placeholder="请输入渠道，如：线上、线下、APP等" size="large" />
          </Form.Item>

          <Form.Item
            name="content"
            label="审查内容"
            rules={[{ required: true, message: '请输入审查内容' }]}
          >
            <TextArea
              rows={12}
              placeholder="请粘贴需要审查的文档内容..."
              style={{ fontSize: 14 }}
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              icon={<SendOutlined />}
              size="large"
              style={{ minWidth: 140 }}
            >
              提交审查
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default ReviewSubmit;
