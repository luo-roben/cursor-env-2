import React, { useEffect, useState } from 'react';
import {
  Card,
  Tabs,
  Tag,
  Badge,
  Descriptions,
  Spin,
  Row,
  Col,
  message,
} from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import { getReviewDetail } from '../../services/api';

interface ReviewIssue {
  id: number;
  severity: string;
  description: string;
  originalText: string;
  suggestion: string;
  suggestionType: string;
  citationStatus: string;
  ruleId?: string;
  cardCategory?: string;
}

interface ReviewCard {
  category: string;
  categoryName: string;
  issues: ReviewIssue[];
  status: string;
  score?: number;
}

interface MissingElement {
  element: string;
  description: string;
  severity: string;
}

interface ReviewData {
  id: number;
  documentType: string;
  contentType: string;
  productType?: string;
  contractType?: string;
  channel?: string;
  content: string;
  verdict: string;
  riskScore: number;
  riskLevel: string;
  status: string;
  createdAt: string;
  reviewCards: ReviewCard[];
  missingElements: MissingElement[];
  issues: ReviewIssue[];
}

const severityConfig: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
  critical: {
    color: '#ff4d4f',
    icon: <CloseCircleOutlined />,
    label: '严重',
  },
  major: {
    color: '#fa8c16',
    icon: <WarningOutlined />,
    label: '重要',
  },
  minor: {
    color: '#faad14',
    icon: <ExclamationCircleOutlined />,
    label: '轻微',
  },
  info: {
    color: '#1890ff',
    icon: <InfoCircleOutlined />,
    label: '提示',
  },
};

const cardCategories = [
  { key: 'text_basic_check', name: '文本基础核对', index: 1 },
  { key: 'redline_sensitive', name: '红线与敏感词', index: 2 },
  { key: 'form_element_review', name: '形式与要素审查', index: 3 },
  { key: 'semantic_compliance', name: '语义与合规审查', index: 4 },
  { key: 'logic_clause_review', name: '逻辑与条款审查', index: 5 },
];

const verdictConfig: Record<string, { text: string; color: string }> = {
  compliant: { text: '合规', color: 'green' },
  violation: { text: '违规', color: 'red' },
  needs_review: { text: '需复核', color: 'orange' },
};

const ReviewDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [data, setData] = useState<ReviewData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (id) fetchData(Number(id));
  }, [id]);

  const fetchData = async (reviewId: number) => {
    try {
      setLoading(true);
      const res = await getReviewDetail(reviewId);
      const result = res.data?.data || res.data;
      setData(result);
    } catch (err) {
      message.error('加载审查详情失败');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!data) {
    return <div style={{ textAlign: 'center', padding: 100, color: '#999' }}>未找到审查记录</div>;
  }

  const getCardIssues = (categoryKey: string): ReviewIssue[] => {
    if (data.reviewCards) {
      const card = data.reviewCards.find(
        (c) => c.category === categoryKey || c.categoryName === categoryKey
      );
      return card?.issues || [];
    }
    if (data.issues) {
      return data.issues.filter((i) => i.cardCategory === categoryKey);
    }
    return [];
  };

  const getCardStatus = (categoryKey: string): string => {
    if (data.reviewCards) {
      const card = data.reviewCards.find(
        (c) => c.category === categoryKey || c.categoryName === categoryKey
      );
      return card?.status || 'pending';
    }
    const issues = getCardIssues(categoryKey);
    if (issues.length === 0) return 'pass';
    if (issues.some((i) => i.severity === 'critical')) return 'critical';
    if (issues.some((i) => i.severity === 'major')) return 'warning';
    return 'info';
  };

  const verdict = verdictConfig[data.verdict] || { text: data.verdict || '-', color: 'default' };

  const tabItems = cardCategories.map((cat) => {
    const issues = getCardIssues(cat.key);
    return {
      key: cat.key,
      label: (
        <span>
          <Badge count={issues.length} size="small" offset={[6, -2]}>
            <span>
              {'\u2460\u2461\u2462\u2463\u2464'[cat.index - 1]} {cat.name}
            </span>
          </Badge>
        </span>
      ),
      children: (
        <div>
          {issues.length === 0 ? (
            <div
              style={{
                textAlign: 'center',
                padding: 40,
                color: '#52c41a',
                fontSize: 16,
              }}
            >
              <CheckCircleOutlined style={{ fontSize: 32, marginBottom: 8 }} />
              <div>该类别无问题发现</div>
            </div>
          ) : (
            <div>
              {issues.map((issue, idx) => {
                const sev = severityConfig[issue.severity] || severityConfig.info;
                return (
                  <Card
                    key={issue.id || idx}
                    size="small"
                    style={{
                      marginBottom: 12,
                      borderLeft: `4px solid ${sev.color}`,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                      <Tag
                        color={sev.color}
                        icon={sev.icon}
                        style={{ flexShrink: 0, marginTop: 2 }}
                      >
                        {sev.label}
                      </Tag>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 8 }}>
                          {issue.description}
                        </div>

                        {issue.originalText && (
                          <div
                            style={{
                              background: '#fff2f0',
                              border: '1px solid #ffccc7',
                              borderRadius: 4,
                              padding: '8px 12px',
                              marginBottom: 8,
                              fontSize: 13,
                            }}
                          >
                            <span style={{ color: '#999', marginRight: 8 }}>原文：</span>
                            <span
                              style={{
                                background: '#ffa39e',
                                padding: '1px 4px',
                                borderRadius: 2,
                              }}
                            >
                              {issue.originalText}
                            </span>
                          </div>
                        )}

                        {issue.suggestion && (
                          <div
                            style={{
                              background: '#f6ffed',
                              border: '1px solid #b7eb8f',
                              borderRadius: 4,
                              padding: '8px 12px',
                              marginBottom: 8,
                              fontSize: 13,
                            }}
                          >
                            <span style={{ color: '#999', marginRight: 8 }}>建议：</span>
                            {issue.suggestion}
                          </div>
                        )}

                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                          {issue.suggestionType && (
                            <Tag color="purple">{issue.suggestionType}</Tag>
                          )}
                          {issue.citationStatus && (
                            <Tag
                              color={
                                issue.citationStatus === 'verified'
                                  ? 'green'
                                  : issue.citationStatus === 'not_found'
                                  ? 'red'
                                  : 'default'
                              }
                            >
                              引用: {issue.citationStatus}
                            </Tag>
                          )}
                          {issue.ruleId && (
                            <Tag color="geekblue">规则: {issue.ruleId}</Tag>
                          )}
                        </div>
                      </div>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}
        </div>
      ),
    };
  });

  const cardStatusIcon = (status: string) => {
    switch (status) {
      case 'pass':
        return <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 20 }} />;
      case 'critical':
        return <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 20 }} />;
      case 'warning':
        return <WarningOutlined style={{ color: '#fa8c16', fontSize: 20 }} />;
      case 'info':
        return <InfoCircleOutlined style={{ color: '#1890ff', fontSize: 20 }} />;
      default:
        return <ExclamationCircleOutlined style={{ color: '#d9d9d9', fontSize: 20 }} />;
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>审查详情 #{data.id}</h2>

      <Card style={{ marginBottom: 16 }}>
        <Descriptions column={4} size="small">
          <Descriptions.Item label="任务ID">{data.id}</Descriptions.Item>
          <Descriptions.Item label="文档类型">{data.documentType}</Descriptions.Item>
          <Descriptions.Item label="内容类型">{data.contentType}</Descriptions.Item>
          <Descriptions.Item label="结论">
            <Tag color={verdict.color} style={{ fontWeight: 600 }}>
              {verdict.text}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="风险分">
            <span style={{ fontWeight: 700, fontSize: 16, color: '#ff4d4f' }}>
              {data.riskScore ?? '-'}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="风险等级">
            <Tag
              color={
                data.riskLevel === 'critical'
                  ? 'red'
                  : data.riskLevel === 'high'
                  ? 'orange'
                  : data.riskLevel === 'medium'
                  ? 'gold'
                  : 'green'
              }
            >
              {data.riskLevel || '-'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={data.status === 'completed' ? 'green' : 'blue'}>
              {data.status || '-'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {data.createdAt ? new Date(data.createdAt).toLocaleString('zh-CN') : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Row gutter={16}>
        <Col span={16}>
          <Card title="审查卡片" style={{ marginBottom: 16 }}>
            <Tabs items={tabItems} type="card" />
          </Card>

          {data.missingElements && data.missingElements.length > 0 && (
            <Card title="缺失要素" style={{ marginBottom: 16 }}>
              {data.missingElements.map((el, idx) => {
                const sev = severityConfig[el.severity] || severityConfig.info;
                return (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 12,
                      padding: '8px 0',
                      borderBottom: '1px solid #f0f0f0',
                    }}
                  >
                    <Tag color={sev.color}>{sev.label}</Tag>
                    <span style={{ fontWeight: 600 }}>{el.element}</span>
                    <span style={{ color: '#666' }}>{el.description}</span>
                  </div>
                );
              })}
            </Card>
          )}

          <Card title="卡片总览">
            <Row gutter={[16, 16]}>
              {cardCategories.map((cat) => {
                const issues = getCardIssues(cat.key);
                const status = getCardStatus(cat.key);
                return (
                  <Col span={4} key={cat.key}>
                    <div
                      style={{
                        textAlign: 'center',
                        padding: 16,
                        background: '#fafafa',
                        borderRadius: 8,
                        border: '1px solid #f0f0f0',
                      }}
                    >
                      {cardStatusIcon(status)}
                      <div style={{ marginTop: 8, fontSize: 12, fontWeight: 600 }}>
                        {cat.name}
                      </div>
                      <div style={{ marginTop: 4, fontSize: 11, color: '#999' }}>
                        {issues.length} 个问题
                      </div>
                    </div>
                  </Col>
                );
              })}
            </Row>
          </Card>
        </Col>

        <Col span={8}>
          <Card
            title={
              <span>
                <FileTextOutlined style={{ marginRight: 8 }} />
                原始内容
              </span>
            }
            style={{ position: 'sticky', top: 24 }}
          >
            <div
              style={{
                maxHeight: 600,
                overflow: 'auto',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
                fontSize: 13,
                lineHeight: 1.8,
                color: '#333',
                background: '#fafafa',
                padding: 16,
                borderRadius: 4,
              }}
            >
              {data.content || '暂无内容'}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ReviewDetail;
